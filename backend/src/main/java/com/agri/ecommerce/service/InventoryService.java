package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.InventoryAdjustmentRequest;
import com.agri.ecommerce.dto.request.InventoryImportRequest;
import com.agri.ecommerce.dto.response.*;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InventoryService {
    private static final int LOW_STOCK_THRESHOLD = 10;
    private final ProductRepository products;
    private final InventoryBatchRepository batches;
    private final InventoryTransactionRepository transactions;
    private final OrderInventoryAllocationRepository allocations;
    private final SupplierRepository suppliers;
    private final UserRepository users;
    private final int expiryWarningDays;

    public InventoryService(ProductRepository products, InventoryBatchRepository batches,
                            InventoryTransactionRepository transactions, OrderInventoryAllocationRepository allocations,
                            SupplierRepository suppliers, UserRepository users,
                            @Value("${app.inventory.expiry-warning-days:3}") int expiryWarningDays) {
        this.products=products; this.batches=batches; this.transactions=transactions;
        this.allocations=allocations; this.suppliers=suppliers; this.users=users;
        this.expiryWarningDays=Math.max(0,expiryWarningDays);
    }

    @Transactional(readOnly=true)
    public InventorySummaryResponse summary() {
        LocalDate today=today();
        Map<Long,List<InventoryBatch>> byProduct=batches.findAllByOrderByProduct_IdAscIdAsc().stream()
            .collect(Collectors.groupingBy(batch -> batch.getProduct().getId()));
        long inStock=0, lowStock=0, expiring=0, expired=0;
        for(Product product:products.findAll()) {
            List<InventoryBatch> stock=byProduct.getOrDefault(product.getId(),List.of());
            long available=stock.stream().mapToLong(batch -> available(batch,today)).sum();
            if(available>0) inStock++;
            if(available>0 && available<=LOW_STOCK_THRESHOLD) lowStock++;
            expiring+=stock.stream().filter(batch -> batch.getRemainingQuantity()>0 && "EXPIRING".equals(expiryStatus(batch,today))).count();
            expired+=stock.stream().filter(batch -> batch.getRemainingQuantity()>0 && "EXPIRED".equals(expiryStatus(batch,today))).count();
        }
        return new InventorySummaryResponse(inStock,lowStock,expiring,expired);
    }

    @Transactional(readOnly=true)
    public PageResponse<InventoryProductResponse> inventory(String search,String status,String expiry,Pageable pageable) {
        LocalDate today=today();
        Map<Long,List<InventoryBatch>> byProduct=batches.findAllByOrderByProduct_IdAscIdAsc().stream()
            .collect(Collectors.groupingBy(batch -> batch.getProduct().getId()));
        String query=search==null?"":search.trim().toLowerCase(Locale.ROOT);
        List<InventoryProductResponse> filtered=products.findAll().stream()
            .map(product -> toInventoryProduct(product,byProduct.getOrDefault(product.getId(),List.of()),today))
            .filter(item -> query.isEmpty() || item.productName().toLowerCase(Locale.ROOT).contains(query)
                || item.productCode().toLowerCase(Locale.ROOT).contains(query)
                || item.productId().toString().equals(query))
            .filter(item -> !StringUtils.hasText(status) || item.stockStatus().equalsIgnoreCase(status))
            .filter(item -> !StringUtils.hasText(expiry) || switch(expiry.toUpperCase(Locale.ROOT)) {
                case "EXPIRING" -> item.hasExpiringBatch();
                case "EXPIRED" -> item.hasExpiredBatch();
                case "VALID" -> !item.hasExpiringBatch() && !item.hasExpiredBatch();
                default -> false;
            }).sorted(inventorySort(pageable.getSort())).toList();
        int start=(int)Math.min((long)filtered.size(),pageable.getOffset());
        int end=Math.min(filtered.size(),start+pageable.getPageSize());
        int pages=(int)Math.ceil((double)filtered.size()/pageable.getPageSize());
        return new PageResponse<>(filtered.subList(start,end),pageable.getPageNumber(),pageable.getPageSize(),filtered.size(),pages,pageable.getPageNumber()==0,pageable.getPageNumber()>=pages-1);
    }

    @Transactional(readOnly=true)
    public InventoryDetailResponse detail(Long productId) {
        Product product=products.findById(productId).orElseThrow(() -> notFound("PRODUCT_NOT_FOUND","Không tìm thấy sản phẩm"));
        List<InventoryBatch> stock=batches.findAllByProduct_IdOrderByExpiryDateAscIdAsc(productId);
        LocalDate today=today();
        return new InventoryDetailResponse(toInventoryProduct(product,stock,today),stock.stream().map(batch -> toBatch(batch,today)).toList());
    }

    @Transactional(readOnly=true)
    public PageResponse<InventoryBatchResponse> batchPage(Long productId,String search,String expiry,Pageable pageable) {
        LocalDate today=today();
        Specification<InventoryBatch> spec=(root,query,builder)->{
            List<Predicate> predicates=new ArrayList<>();
            if(productId!=null) predicates.add(builder.equal(root.get("product").get("id"),productId));
            if(StringUtils.hasText(search)) {
                String pattern="%"+search.trim().toLowerCase(Locale.ROOT)+"%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("batchCode")),pattern),
                    builder.like(builder.lower(root.get("product").get("name")),pattern)));
            }
            if(StringUtils.hasText(expiry)) switch(expiry.toUpperCase(Locale.ROOT)) {
                case "EXPIRED" -> predicates.add(builder.lessThan(root.get("expiryDate"),today));
                case "EXPIRING" -> predicates.add(builder.and(builder.greaterThanOrEqualTo(root.get("expiryDate"),today),builder.lessThanOrEqualTo(root.get("expiryDate"),today.plusDays(expiryWarningDays))));
                case "VALID" -> predicates.add(builder.or(builder.isNull(root.get("expiryDate")),builder.greaterThan(root.get("expiryDate"),today.plusDays(expiryWarningDays))));
                default -> predicates.add(builder.disjunction());
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return PageResponse.from(batches.findAll(spec,pageable),batch -> toBatch(batch,today));
    }

    @Transactional(readOnly=true)
    public PageResponse<InventoryTransactionResponse> transactionPage(Long productId,String type,String search,LocalDate from,LocalDate to,Pageable pageable) {
        Specification<InventoryTransaction> spec=(root,query,builder)->{
            List<Predicate> predicates=new ArrayList<>();
            if(productId!=null) predicates.add(builder.equal(root.get("product").get("id"),productId));
            if(StringUtils.hasText(type)) predicates.add(builder.equal(root.get("type"),type.trim().toUpperCase(Locale.ROOT)));
            if(StringUtils.hasText(search)) {
                String pattern="%"+search.trim().toLowerCase(Locale.ROOT)+"%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("product").get("name")),pattern),
                    builder.like(builder.lower(root.get("batch").get("batchCode")),pattern),builder.like(builder.lower(root.get("reason")),pattern)));
            }
            if(from!=null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"),from.atStartOfDay()));
            if(to!=null) predicates.add(builder.lessThan(root.get("createdAt"),to.plusDays(1).atStartOfDay()));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return PageResponse.from(transactions.findAll(spec,pageable),this::toTransaction);
    }

    @Transactional
    public InventoryBatchResponse importBatch(InventoryImportRequest request,Long actorId) {
        LocalDate today=today();
        if(request.quantity()<=0) throw invalid("INVALID_QUANTITY","Số lượng nhập phải lớn hơn 0");
        if(request.importPrice()==null || request.importPrice().signum()<0) throw invalid("INVALID_IMPORT_PRICE","Giá nhập không được âm");
        if(request.expiryDate()==null || request.expiryDate().isBefore(today)) throw invalid("BATCH_EXPIRED","Không được nhập lô đã hết hạn");
        if(request.manufactureDate()!=null && !request.expiryDate().isAfter(request.manufactureDate()))
            throw invalid("INVALID_EXPIRY_DATE","Hạn sử dụng phải sau ngày sản xuất hoặc thu hoạch");
        Product product=lockProduct(request.productId());
        Supplier supplier=request.supplierId()==null?null:suppliers.findById(request.supplierId())
            .filter(value -> "active".equals(value.getStatus())).orElseThrow(() -> notFound("SUPPLIER_NOT_FOUND","Không tìm thấy nhà cung cấp đang hoạt động"));
        User actor=actorId==null?null:users.getReferenceById(actorId);
        InventoryBatch batch=new InventoryBatch();
        batch.setBatchCode("GF-"+today.toString().replace("-","")+"-"+UUID.randomUUID().toString().substring(0,12).toUpperCase(Locale.ROOT));
        batch.setProduct(product);batch.setQuantity(request.quantity());batch.setRemainingQuantity(request.quantity());
        batch.setReservedQuantity(0);batch.setUnit(product.getUnit());batch.setImportPrice(request.importPrice());
        batch.setManufactureDate(request.manufactureDate());batch.setExpiryDate(request.expiryDate());batch.setSupplier(supplier);
        batch.setNote(trim(request.note()));batch.setCreatedBy(actor);
        batches.save(batch);
        movement(batch,"IMPORT",request.quantity(),0,0,null,"Nhập kho"+(batch.getNote()==null?"":": "+batch.getNote()),actor);
        refreshProduct(product,batches.findByProductIdForUpdate(product.getId()),today);
        return toBatch(batch,today);
    }

    @Transactional
    public InventoryBatchResponse adjust(InventoryAdjustmentRequest request,Long actorId) {
        if(request.quantityChange()==0) throw invalid("INVALID_QUANTITY","Số lượng điều chỉnh phải khác 0");
        String type=request.type().trim().toUpperCase(Locale.ROOT);
        if(!Set.of("ADJUSTMENT","DAMAGED","EXPIRED","RETURN").contains(type)) throw invalid("INVALID_ADJUSTMENT_TYPE","Loại điều chỉnh không hợp lệ");
        if(("DAMAGED".equals(type)||"EXPIRED".equals(type)) && request.quantityChange()>0)
            throw invalid("INVALID_ADJUSTMENT_QUANTITY","Hàng hỏng hoặc hết hạn phải giảm tồn kho");
        if("RETURN".equals(type) && request.quantityChange()<0)
            throw invalid("INVALID_ADJUSTMENT_QUANTITY","Hàng trả lại phải tăng tồn kho");
        InventoryBatch existing=batches.findById(request.batchId()).orElseThrow(() -> notFound("BATCH_NOT_FOUND","Không tìm thấy lô hàng"));
        Product product=lockProduct(existing.getProduct().getId());
        InventoryBatch batch=batches.findByProductIdForUpdate(product.getId()).stream()
            .filter(value -> value.getId().equals(request.batchId())).findFirst().orElseThrow(() -> notFound("BATCH_NOT_FOUND","Không tìm thấy lô hàng"));
        int before=batch.getRemainingQuantity();
        long after=(long)before+request.quantityChange();
        if(after<batch.getReservedQuantity() || after>Integer.MAX_VALUE)
            throw invalid("INSUFFICIENT_STOCK","Không thể điều chỉnh vượt quá số lượng chưa giữ cho đơn hàng");
        User actor=users.getReferenceById(actorId);
        batch.setRemainingQuantity((int)after);
        movement(batch,type,request.quantityChange(),before,batch.getReservedQuantity(),null,request.reason().trim(),actor);
        refreshProduct(product,batches.findByProductIdForUpdate(product.getId()),today());
        return toBatch(batch,today());
    }

    /** Caller has locked every product in ascending id order and saved the order/items. */
    public void reserveOrder(Order order,Map<Long,Product> lockedProducts) {
        LocalDate today=today();
        for(OrderItem item:order.getItems().stream().sorted(Comparator.comparing(value -> value.getProduct().getId())).toList()) {
            Product product=lockedProducts.get(item.getProduct().getId());
            if(product==null) throw invalid("PRODUCT_NOT_FOUND","Không tìm thấy sản phẩm khi giữ kho");
            List<InventoryBatch> stock=batches.findByProductIdForUpdate(product.getId());
            reserveItem(item,item.getQuantity(),stock,new ArrayList<>(),order.getId(),today);
            refreshProduct(product,stock,today);
        }
    }

    /** Export is idempotent: only allocations still RESERVED are consumed. */
    public void exportOrder(Order order) {
        Map<Long,Product> locked=lockOrderProducts(order);
        List<OrderInventoryAllocation> reserved=allocations.findAllByOrderItem_Order_IdOrderByIdAsc(order.getId());
        if(reserved.isEmpty() && !order.getItems().isEmpty()) throw invalid("INVENTORY_ALLOCATION_MISSING","Đơn hàng chưa có phân bổ lô hàng");
        LocalDate today=today();
        for(OrderItem item:order.getItems().stream().sorted(Comparator.comparing(value -> value.getProduct().getId())).toList()) {
            Product product=locked.get(item.getProduct().getId());
            List<InventoryBatch> stock=batches.findByProductIdForUpdate(product.getId());
            List<OrderInventoryAllocation> itemAllocations=reserved.stream().filter(value -> value.getOrderItem().getId().equals(item.getId())).collect(Collectors.toCollection(ArrayList::new));
            int reallocate=0;
            for(OrderInventoryAllocation allocation:itemAllocations) {
                if(!"RESERVED".equals(allocation.getStatus())) continue;
                InventoryBatch batch=stock.stream().filter(value -> value.getId().equals(allocation.getBatch().getId())).findFirst().orElseThrow();
                if(!usable(batch,today)) {
                    int before=batch.getRemainingQuantity(), beforeReserved=batch.getReservedQuantity();
                    batch.setReservedQuantity(beforeReserved-allocation.getQuantity());
                    allocation.setStatus("RELEASED");
                    movement(batch,"RESERVATION_RELEASE",-allocation.getQuantity(),before,beforeReserved,order.getId(),"Lô hết hạn trước khi xử lý đơn",null);
                    reallocate+=allocation.getQuantity();
                }
            }
            if(reallocate>0) reserveItem(item,reallocate,stock,itemAllocations,order.getId(),today);
            for(OrderInventoryAllocation allocation:itemAllocations) {
                if(!"RESERVED".equals(allocation.getStatus())) continue;
                InventoryBatch batch=stock.stream().filter(value -> value.getId().equals(allocation.getBatch().getId())).findFirst().orElseThrow();
                if(!usable(batch,today) || batch.getRemainingQuantity()<allocation.getQuantity() || batch.getReservedQuantity()<allocation.getQuantity())
                    throw invalid("INSUFFICIENT_STOCK","Không đủ hàng còn hạn để xử lý đơn");
                int before=batch.getRemainingQuantity(),beforeReserved=batch.getReservedQuantity();
                batch.setRemainingQuantity(before-allocation.getQuantity());
                batch.setReservedQuantity(beforeReserved-allocation.getQuantity());
                allocation.setStatus("EXPORTED");allocation.setExportedAt(LocalDateTime.now());
                movement(batch,"EXPORT",-allocation.getQuantity(),before,beforeReserved,order.getId(),"Xuất theo FEFO cho đơn hàng",null);
            }
            refreshProduct(product,stock,today);
        }
    }

    public void restoreOrder(Order order) {
        Map<Long,Product> locked=lockOrderProducts(order);
        List<OrderInventoryAllocation> history=allocations.findAllByOrderItem_Order_IdOrderByIdAsc(order.getId());
        if(history.isEmpty() && !order.getItems().isEmpty()) throw invalid("INVENTORY_ALLOCATION_MISSING","Đơn hàng chưa có phân bổ lô hàng");
        LocalDate today=today();
        for(Product product:locked.values().stream().sorted(Comparator.comparing(Product::getId)).toList()) {
            List<InventoryBatch> stock=batches.findByProductIdForUpdate(product.getId());
            for(OrderInventoryAllocation allocation:history) {
                InventoryBatch batch=stock.stream().filter(value -> value.getId().equals(allocation.getBatch().getId())).findFirst().orElse(null);
                if(batch==null) continue;
                int before=batch.getRemainingQuantity(), beforeReserved=batch.getReservedQuantity();
                if("RESERVED".equals(allocation.getStatus())) {
                    batch.setReservedQuantity(beforeReserved-allocation.getQuantity());
                    allocation.setStatus("RELEASED");allocation.setRestoredAt(LocalDateTime.now());
                    movement(batch,"RESERVATION_RELEASE",-allocation.getQuantity(),before,beforeReserved,order.getId(),"Hủy đơn, trả số lượng đã giữ",null);
                } else if("EXPORTED".equals(allocation.getStatus())) {
                    batch.setRemainingQuantity(before+allocation.getQuantity());
                    allocation.setStatus("RESTORED");allocation.setRestoredAt(LocalDateTime.now());
                    movement(batch,"ORDER_CANCEL_RESTORE",allocation.getQuantity(),before,beforeReserved,order.getId(),"Hủy đơn, hoàn đúng lô đã xuất",null);
                }
            }
            refreshProduct(product,stock,today);
        }
    }

    @Transactional
    public void syncExpiredProductStock() {
        LocalDate today=today();
        for(Long productId:batches.findExpiredProductIds(today)) {
            Product product=lockProduct(productId);
            refreshProduct(product,batches.findByProductIdForUpdate(productId),today);
        }
    }

    private void reserveItem(OrderItem item,int amount,List<InventoryBatch> stock,List<OrderInventoryAllocation> existing,Long orderId,LocalDate today) {
        int needed=amount;
        for(InventoryBatch batch:stock) {
            if(needed==0) break;
            if(!usable(batch,today)) continue;
            int take=Math.min(needed,batch.getRemainingQuantity()-batch.getReservedQuantity());
            if(take<=0) continue;
            int before=batch.getRemainingQuantity(),beforeReserved=batch.getReservedQuantity();
            batch.setReservedQuantity(beforeReserved+take);
            OrderInventoryAllocation allocation=existing.stream().filter(value -> "RESERVED".equals(value.getStatus()) && value.getBatch().getId().equals(batch.getId())).findFirst().orElse(null);
            if(allocation==null) {
                allocation=new OrderInventoryAllocation();allocation.setOrderItem(item);allocation.setBatch(batch);allocation.setQuantity(take);allocation.setStatus("RESERVED");
                allocations.save(allocation);existing.add(allocation);
            } else allocation.setQuantity(allocation.getQuantity()+take);
            movement(batch,"RESERVE",take,before,beforeReserved,orderId,"Giữ hàng cho đơn hàng",null);
            needed-=take;
        }
        if(needed>0) throw invalid("INSUFFICIENT_STOCK","Không đủ hàng còn hạn trong kho để xử lý đơn");
    }

    private Map<Long,Product> lockOrderProducts(Order order) {
        List<Long> ids=order.getItems().stream().map(item -> item.getProduct().getId()).distinct().sorted().toList();
        return ids.isEmpty()?Map.of():products.findAllByIdForUpdate(ids).stream().collect(Collectors.toMap(Product::getId,Function.identity()));
    }

    private Product lockProduct(Long productId) {
        return products.findAllByIdForUpdate(List.of(productId)).stream().findFirst()
            .orElseThrow(() -> notFound("PRODUCT_NOT_FOUND","Không tìm thấy sản phẩm"));
    }

    private void refreshProduct(Product product,List<InventoryBatch> stock,LocalDate today) {
        long available=stock.stream().mapToLong(batch -> available(batch,today)).sum();
        product.setStock(Math.toIntExact(available));
        if(product.getStatus()!=ProductStatus.HIDDEN)
            product.setStatus(available>0?ProductStatus.IN_STOCK:ProductStatus.OUT_OF_STOCK);
        products.save(product);
    }

    private int available(InventoryBatch batch,LocalDate today) {
        return usable(batch,today)?Math.max(0,batch.getRemainingQuantity()-batch.getReservedQuantity()):0;
    }
    private boolean usable(InventoryBatch batch,LocalDate today) {return batch.getExpiryDate()==null || !batch.getExpiryDate().isBefore(today);}
    private String expiryStatus(InventoryBatch batch,LocalDate today) {
        if(batch.getExpiryDate()==null)return "NO_EXPIRY";
        if(batch.getExpiryDate().isBefore(today))return "EXPIRED";
        return batch.getExpiryDate().isAfter(today.plusDays(expiryWarningDays))?"VALID":"EXPIRING";
    }

    private InventoryProductResponse toInventoryProduct(Product product,List<InventoryBatch> stock,LocalDate today) {
        long total=stock.stream().mapToLong(InventoryBatch::getRemainingQuantity).sum();
        long available=stock.stream().mapToLong(batch -> available(batch,today)).sum();
        String status=available==0?"OUT_OF_STOCK":available<=LOW_STOCK_THRESHOLD?"LOW_STOCK":"IN_STOCK";
        return new InventoryProductResponse(product.getId(),"GF-P"+product.getId(),product.getName(),product.getCategory().getName(),product.getUnit(),
            total,available,status,
            stock.stream().anyMatch(batch -> batch.getRemainingQuantity()>0 && "EXPIRING".equals(expiryStatus(batch,today))),
            stock.stream().anyMatch(batch -> batch.getRemainingQuantity()>0 && "EXPIRED".equals(expiryStatus(batch,today))),stock.size());
    }
    private Comparator<InventoryProductResponse> inventorySort(Sort sort) {
        Comparator<InventoryProductResponse> result=null;
        for(Sort.Order order:sort) {
            Comparator<InventoryProductResponse> part=switch(order.getProperty()) {
                case "productCode" -> Comparator.comparing(InventoryProductResponse::productCode);
                case "availableQuantity" -> Comparator.comparingLong(InventoryProductResponse::availableQuantity);
                case "totalQuantity" -> Comparator.comparingLong(InventoryProductResponse::totalQuantity);
                case "batchCount" -> Comparator.comparingLong(InventoryProductResponse::batchCount);
                case "productName" -> Comparator.comparing(InventoryProductResponse::productName,String.CASE_INSENSITIVE_ORDER);
                default -> null;
            };
            if(part!=null) result=result==null?(order.isDescending()?part.reversed():part):result.thenComparing(order.isDescending()?part.reversed():part);
        }
        return (result==null?Comparator.comparing(InventoryProductResponse::productName,String.CASE_INSENSITIVE_ORDER):result)
            .thenComparing(InventoryProductResponse::productId);
    }
    private InventoryBatchResponse toBatch(InventoryBatch batch,LocalDate today) {
        Supplier supplier=batch.getSupplier();
        return new InventoryBatchResponse(batch.getId(),batch.getBatchCode(),batch.getProduct().getId(),batch.getProduct().getName(),
            batch.getQuantity(),batch.getRemainingQuantity(),batch.getReservedQuantity(),available(batch,today),batch.getUnit(),batch.getImportPrice(),
            batch.getManufactureDate(),batch.getExpiryDate(),supplier==null?null:supplier.getId(),supplier==null?null:supplier.getName(),
            batch.getImportedAt(),batch.getNote(),batch.getCreatedBy()==null?null:batch.getCreatedBy().getId(),expiryStatus(batch,today));
    }
    private InventoryTransactionResponse toTransaction(InventoryTransaction value) {
        User actor=value.getCreatedBy();
        return new InventoryTransactionResponse(value.getId(),value.getProduct().getId(),value.getProduct().getName(),
            value.getBatch().getId(),value.getBatch().getBatchCode(),value.getType(),value.getQuantity(),value.getQuantityBefore(),value.getQuantityAfter(),
            value.getReservedBefore(),value.getReservedAfter(),value.getReferenceId(),value.getReason(),actor==null?null:actor.getId(),actor==null?null:actor.getName(),value.getCreatedAt());
    }
    private void movement(InventoryBatch batch,String type,int quantity,int before,int reservedBefore,Long referenceId,String reason,User actor) {
        InventoryTransaction value=new InventoryTransaction();
        value.setProduct(batch.getProduct());value.setBatch(batch);value.setType(type);value.setQuantity(quantity);
        value.setQuantityBefore(before);value.setQuantityAfter(batch.getRemainingQuantity());
        value.setReservedBefore(reservedBefore);value.setReservedAfter(batch.getReservedQuantity());
        value.setReferenceId(referenceId);value.setReason(reason);value.setCreatedBy(actor);
        transactions.save(value);
    }
    private LocalDate today(){return LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));}
    private String trim(String value){return StringUtils.hasText(value)?value.trim():null;}
    private ApplicationException invalid(String code,String message){return new ApplicationException(HttpStatus.CONFLICT,code,message);}
    private ApplicationException notFound(String code,String message){return new ApplicationException(HttpStatus.NOT_FOUND,code,message);}
}

