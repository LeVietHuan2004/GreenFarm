package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.entity.PaymentMethod;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class VnpayRefundService {
    private static final DateTimeFormatter FORMAT=DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN=ZoneId.of("Asia/Ho_Chi_Minh");
    private final VnpayRefundPersistenceService persistence;
    private final RestClient client=RestClient.create();
    private final ObjectMapper json=new ObjectMapper();
    private final String tmnCode; private final String secret; private final String refundUrl;
    public VnpayRefundService(VnpayRefundPersistenceService persistence,
                              @Value("${app.payment.vnpay.tmn-code:}") String tmnCode,
                              @Value("${app.payment.vnpay.hash-secret:}") String secret,
                              @Value("${app.payment.vnpay.refund-url:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}") String refundUrl) {
        this.persistence=persistence; this.tmnCode=tmnCode; this.secret=secret; this.refundUrl=refundUrl;
    }

    public RefundOutcome refund(Long orderId, BigDecimal requestedAmount, String actor, String clientIp) {
        var attempt=persistence.prepare(orderId, requestedAmount, actor);
        if (attempt.payment().getPaymentMethod()!=PaymentMethod.VNPAY) return RefundOutcome.local();
        if (attempt.alreadyFinal()) return RefundOutcome.fullSuccess(attempt.payment().getAmount(), "Giao dich da hoan tien");
        if (attempt.processing()) return RefundOutcome.processing(attempt.refund().getRefundAmount(), "Yeu cau hoan tien dang duoc VNPAY xu ly");
        if (tmnCode.isBlank() || secret.isBlank()) throw new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, "VNPAY_NOT_CONFIGURED", "Chua cau hinh VNPAY_TMN_CODE hoac VNPAY_HASH_SECRET");
        var payment=attempt.payment(); var refund=attempt.refund();
        String amount=refund.getRefundAmount().movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).toPlainString();
        String createDate=LocalDateTime.now(VN).format(FORMAT);
        String ip=clientIp == null || clientIp.isBlank() ? "127.0.0.1" : clientIp;
        String info="Hoan tien don hang " + orderId;
        LocalDateTime originalRequestDate = payment.getVnpayPaymentRequestDate() == null ? payment.getCreatedAt() : payment.getVnpayPaymentRequestDate();
        String transactionDate=originalRequestDate.atZone(VN).format(FORMAT);
        String checksum=String.join("|", refund.getRequestId(), "2.1.0", "refund", tmnCode, refund.getTransactionType(),
            payment.getReferenceCode(), amount, payment.getTransactionId(), transactionDate, refund.getRequestedBy(), createDate, ip, info);
        Map<String,String> request=new LinkedHashMap<>();
        request.put("vnp_RequestId", refund.getRequestId()); request.put("vnp_Version", "2.1.0"); request.put("vnp_Command", "refund");
        request.put("vnp_TmnCode", tmnCode); request.put("vnp_TransactionType", refund.getTransactionType()); request.put("vnp_TxnRef", payment.getReferenceCode());
        request.put("vnp_Amount", amount); request.put("vnp_TransactionNo", payment.getTransactionId()); request.put("vnp_TransactionDate", transactionDate);
        request.put("vnp_CreateBy", refund.getRequestedBy()); request.put("vnp_CreateDate", createDate); request.put("vnp_IpAddr", ip); request.put("vnp_OrderInfo", info); request.put("vnp_SecureHash", hmac(checksum));
        Map<String,Object> response;
        try { response=client.post().uri(refundUrl).body(request).retrieve().body(Map.class); }
        catch (RuntimeException exception) { throw new ApplicationException(HttpStatus.BAD_GATEWAY, "VNPAY_REFUND_UNAVAILABLE", "Khong the ket noi Refund API cua VNPAY"); }
        if (response==null) throw new ApplicationException(HttpStatus.BAD_GATEWAY, "VNPAY_REFUND_EMPTY_RESPONSE", "VNPAY khong tra du lieu hoan tien");
        String code=value(response,"vnp_ResponseCode"), status=value(response,"vnp_TransactionStatus"), transactionId=value(response,"vnp_TransactionNo");
        boolean signed=verifyResponse(response);
        persistence.recordResponse(refund.getId(), code, status, transactionId, stringify(response), signed);
        if (!signed) throw new ApplicationException(HttpStatus.BAD_GATEWAY, "VNPAY_REFUND_INVALID_SIGNATURE", "Chu ky phan hoi hoan tien VNPAY khong hop le");
        if ("00".equals(code) && "00".equals(status)) return "02".equals(refund.getTransactionType()) ? RefundOutcome.fullSuccess(refund.getRefundAmount(), "VNPAY da hoan tien thanh cong") : RefundOutcome.partialSuccess(refund.getRefundAmount());
        if ("00".equals(code) || "94".equals(code) || "05".equals(status) || "06".equals(status)) return RefundOutcome.processing(refund.getRefundAmount(), "Yeu cau hoan tien da duoc VNPAY tiep nhan va dang xu ly");
        throw new ApplicationException(HttpStatus.BAD_GATEWAY, "VNPAY_REFUND_REJECTED", "VNPAY tu choi hoan tien (ma " + code + ")");
    }

    private boolean verifyResponse(Map<String,Object> response) {
        String supplied=value(response,"vnp_SecureHash"); if (supplied.isBlank()) return false;
        String data=String.join("|", value(response,"vnp_ResponseId"), value(response,"vnp_Command"), value(response,"vnp_ResponseCode"), value(response,"vnp_Message"), value(response,"vnp_TmnCode"), value(response,"vnp_TxnRef"), value(response,"vnp_Amount"), value(response,"vnp_BankCode"), value(response,"vnp_PayDate"), value(response,"vnp_TransactionNo"), value(response,"vnp_TransactionType"), value(response,"vnp_TransactionStatus"), value(response,"vnp_OrderInfo"));
        return constantTimeEquals(hmac(data), supplied);
    }
    private String value(Map<String,Object> values,String key){Object value=values.get(key);return value == null || "null".equals(value.toString()) ? "" : value.toString();}
    private String stringify(Map<String,Object> values){try{return json.writeValueAsString(values);}catch(JsonProcessingException exception){return values.toString();}}
    private String hmac(String value){try{Mac mac=Mac.getInstance("HmacSHA512");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA512"));StringBuilder out=new StringBuilder();for(byte item:mac.doFinal(value.getBytes(StandardCharsets.UTF_8)))out.append(String.format("%02x",item));return out.toString();}catch(Exception exception){throw new IllegalStateException("Khong the ky yeu cau VNPAY",exception);}}
    private boolean constantTimeEquals(String left,String right){if(left.length()!=right.length())return false;int difference=0;for(int i=0;i<left.length();i++)difference|=left.charAt(i)^right.charAt(i);return difference==0;}
    public record RefundOutcome(boolean gatewayRefund, boolean fullSuccess, boolean processing, BigDecimal amount, String message) {
        static RefundOutcome local(){return new RefundOutcome(false,true,false,BigDecimal.ZERO,"Da ghi nhan hoan tien noi bo");}
        static RefundOutcome fullSuccess(BigDecimal amount,String message){return new RefundOutcome(true,true,false,amount,message);}
        static RefundOutcome partialSuccess(BigDecimal amount){return new RefundOutcome(true,false,false,amount,"VNPAY da hoan tien mot phan thanh cong");}
        static RefundOutcome processing(BigDecimal amount,String message){return new RefundOutcome(true,false,true,amount,message);}
    }
}
