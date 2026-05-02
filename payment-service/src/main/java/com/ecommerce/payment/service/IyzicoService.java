package com.ecommerce.payment.service;

import com.iyzipay.Options;
import com.iyzipay.model.*;
import com.iyzipay.request.CreateCheckoutFormInitializeRequest;
import com.iyzipay.request.RetrieveCheckoutFormRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IyzicoService {

    private final Options options;

    public CheckoutFormInitialize initCheckoutForm(
            Long orderId, BigDecimal amount, Long userId, String userEmail, String callbackUrl) {

        BigDecimal scaledAmount = amount.setScale(2, RoundingMode.HALF_UP);

        CreateCheckoutFormInitializeRequest request = new CreateCheckoutFormInitializeRequest();
        request.setLocale(Locale.TR.getValue());
        request.setConversationId(String.valueOf(orderId));
        request.setPrice(scaledAmount);
        request.setPaidPrice(scaledAmount);
        request.setCurrency(Currency.TRY.name());
        request.setBasketId("basket-" + orderId);
        request.setPaymentGroup(PaymentGroup.PRODUCT.name());
        request.setCallbackUrl(callbackUrl);
        request.setEnabledInstallments(List.of(2, 3, 6, 9));

        Buyer buyer = new Buyer();
        buyer.setId(String.valueOf(userId));
        buyer.setName("Test");
        buyer.setSurname("Kullanici");
        buyer.setIdentityNumber("74300864791");
        buyer.setEmail(userEmail);
        buyer.setGsmNumber("+905350000000");
        buyer.setRegistrationAddress("Barbaros Mahallesi Bestekar Sok. No:2 Balmumcu");
        buyer.setIp("85.34.78.112");
        buyer.setCity("Istanbul");
        buyer.setCountry("Turkey");
        buyer.setZipCode("34349");
        request.setBuyer(buyer);

        Address address = new Address();
        address.setContactName("Test Kullanici");
        address.setCity("Istanbul");
        address.setCountry("Turkey");
        address.setAddress("Barbaros Mahallesi Bestekar Sok. No:2 Balmumcu");
        address.setZipCode("34349");
        request.setBillingAddress(address);
        request.setShippingAddress(address);

        BasketItem item = new BasketItem();
        item.setId("order-" + orderId);
        item.setName("Siparis #" + orderId);
        item.setCategory1("Genel");
        item.setItemType(BasketItemType.PHYSICAL.name());
        item.setPrice(scaledAmount);
        request.setBasketItems(List.of(item));

        log.info("Iyzico checkout baslatiliyor: orderId={}, amount={}", orderId, scaledAmount);
        CheckoutFormInitialize result = CheckoutFormInitialize.create(request, options);
        log.info("Iyzico checkout yaniti: status={}, errorCode={}", result.getStatus(), result.getErrorCode());
        return result;
    }

    public CheckoutForm retrieveResult(String token) {
        RetrieveCheckoutFormRequest request = new RetrieveCheckoutFormRequest();
        request.setLocale(Locale.TR.getValue());
        request.setToken(token);
        return CheckoutForm.retrieve(request, options);
    }
}
