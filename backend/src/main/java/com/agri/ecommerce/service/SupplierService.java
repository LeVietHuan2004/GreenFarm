package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.SupplierRequest;
import com.agri.ecommerce.dto.response.PageResponse;
import com.agri.ecommerce.dto.response.SupplierResponse;
import com.agri.ecommerce.entity.Supplier;
import com.agri.ecommerce.repository.SupplierRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SupplierService {
    private final SupplierRepository suppliers;
    public SupplierService(SupplierRepository suppliers){this.suppliers=suppliers;}

    @Transactional(readOnly=true)
    public PageResponse<SupplierResponse> list(String search,String status,Pageable pageable){
        Specification<Supplier> spec=(root,query,builder)->{
            List<Predicate> predicates=new ArrayList<>();
            if(StringUtils.hasText(search)){
                String pattern="%"+search.trim().toLowerCase(Locale.ROOT)+"%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("name")),pattern),builder.like(builder.lower(root.get("supplierCode")),pattern)));
            }
            if(StringUtils.hasText(status))predicates.add(builder.equal(root.get("status"),status.trim().toLowerCase(Locale.ROOT)));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return PageResponse.from(suppliers.findAll(spec,pageable),this::response);
    }

    @Transactional(readOnly=true)
    public SupplierResponse findOne(Long id){return response(require(id));}

    @Transactional
    public SupplierResponse create(SupplierRequest request){
        String code=normalize(request.supplierCode());
        if(suppliers.existsBySupplierCodeIgnoreCase(code))throw conflict("SUPPLIER_CODE_EXISTS","Mã nhà cung cấp đã tồn tại");
        Supplier supplier=new Supplier();apply(supplier,request,code);
        return response(suppliers.save(supplier));
    }

    @Transactional
    public SupplierResponse update(Long id,SupplierRequest request){
        Supplier supplier=require(id);String code=normalize(request.supplierCode());
        if(suppliers.existsBySupplierCodeIgnoreCaseAndIdNot(code,id))throw conflict("SUPPLIER_CODE_EXISTS","Mã nhà cung cấp đã tồn tại");
        apply(supplier,request,code);return response(suppliers.save(supplier));
    }

    @Transactional
    public SupplierResponse deactivate(Long id){Supplier supplier=require(id);supplier.setStatus("inactive");return response(suppliers.save(supplier));}

    private Supplier require(Long id){return suppliers.findById(id).orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND,"SUPPLIER_NOT_FOUND","Không tìm thấy nhà cung cấp"));}
    private void apply(Supplier value,SupplierRequest request,String code){
        String status=request.status().trim().toLowerCase(Locale.ROOT);
        if(!status.equals("active")&&!status.equals("inactive"))throw conflict("INVALID_SUPPLIER_STATUS","Trạng thái nhà cung cấp không hợp lệ");
        value.setSupplierCode(code);value.setName(request.name().trim());value.setPhone(trim(request.phone()));
        value.setEmail(trim(request.email()));value.setAddress(trim(request.address()));value.setStatus(status);value.setNote(trim(request.note()));
    }
    private String normalize(String code){return code.trim().toUpperCase(Locale.ROOT);}
    private String trim(String value){return StringUtils.hasText(value)?value.trim():null;}
    private SupplierResponse response(Supplier value){return new SupplierResponse(value.getId(),value.getSupplierCode(),value.getName(),value.getPhone(),value.getEmail(),value.getAddress(),value.getStatus(),value.getNote(),value.getCreatedAt(),value.getUpdatedAt());}
    private ApplicationException conflict(String code,String message){return new ApplicationException(HttpStatus.CONFLICT,code,message);}
}
