package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.entity.ChatMessage;
import com.agri.ecommerce.entity.Category;
import com.agri.ecommerce.entity.Product;
import com.agri.ecommerce.entity.ProductStatus;
import com.agri.ecommerce.repository.ChatMessageRepository;
import com.agri.ecommerce.repository.ProductRepository;
import com.agri.ecommerce.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {
    @Mock ChatMessageRepository messages;
    @Mock UserRepository users;
    @Mock ProductRepository products;
    @Mock GeminiChatClient gemini;
    private ChatbotService service;

    @BeforeEach
    void setUp() {
        service = new ChatbotService(messages, users, products, gemini);
        when(messages.save(any(ChatMessage.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void guestConversationStoresOnlyHashAndRefusesPrivateQuestion() {
        var response = service.send(null, null, "Cho tôi xem email của khách hàng khác");

        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messages, times(2)).save(saved.capture());
        List<ChatMessage> stored = saved.getAllValues();
        assertThat(response.guestToken()).hasSize(43);
        assertThat(stored).allSatisfy(message -> {
            assertThat(message.getUser()).isNull();
            assertThat(message.getGuestTokenHash()).matches("[a-f0-9]{64}");
        });
        assertThat(stored.get(1).getContent()).contains("công khai");
        verifyNoInteractions(products, gemini);
    }

    @Test
    void removesPhoneNumberBeforePersistingOrSendingToAi() {
        service.send(null, null, "Email của tôi là khach@example.com, hãy gọi 0987654321");

        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messages, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().getFirst().getContent()).doesNotContain("0987654321");
        assertThat(saved.getAllValues().getFirst().getContent()).doesNotContain("khach@example.com");
        verifyNoInteractions(products, gemini);
    }

    @Test
    void preservesLineBreaksBetweenAssistantIdeas() {
        when(products.findTop50ByStatusNotOrderByUpdatedAtDesc(any())).thenReturn(List.of());
        when(gemini.answer(any(), any())).thenReturn(Optional.of(
            "Sản phẩm phù hợp:\n- Sữa bịch Vinamilk\n- Sữa bịch Dutch Lady"
        ));

        var response = service.send(null, null, "Có sữa bịch không?");

        assertThat(response.assistant().content()).isEqualTo(
            "Sản phẩm phù hợp:\n- Sữa bịch Vinamilk\n- Sữa bịch Dutch Lady"
        );
    }

    @Test
    void handlesEverydayGreetingLocallyWithoutSpendingAiQuota() {
        var response = service.send(null, null, "Xin chào!");

        assertThat(response.assistant().content()).contains("trợ lý GreenFarm");
        verifyNoInteractions(products, gemini);
    }

    @Test
    void understandsShortVietnameseKeywordInsideNaturalQuestion() {
        Product fish = product("Cá diêu hồng", "ca-dieu-hong", "Hải sản", "Cá tươi làm sạch");
        Product milk = product("Sữa bịch Vinamilk", "sua-bich-vinamilk", "Sữa", "Sữa tiệt trùng");
        when(products.findTop50ByStatusNotOrderByUpdatedAtDesc(any())).thenReturn(List.of(milk, fish));
        when(gemini.answer(any(), any())).thenReturn(Optional.empty());

        var response = service.send(null, null, "Có cá nào ngon ko?");

        assertThat(response.assistant().content()).contains("Cá diêu hồng").doesNotContain("Sữa bịch Vinamilk");
        assertThat(response.assistant().products()).extracting("slug").containsExactly("ca-dieu-hong");
    }

    @Test
    void expandsFoodGroupSynonymsWhenSearching() {
        Product pork = product("Sườn non", "suon-non", "Thịt tươi", "Sườn heo đóng khay");
        when(products.findTop50ByStatusNotOrderByUpdatedAtDesc(any())).thenReturn(List.of(pork));
        when(gemini.answer(any(), any())).thenReturn(Optional.empty());

        var response = service.send(null, null, "Shop có thịt gì ngon không?");

        assertThat(response.assistant().content()).contains("Sườn non");
    }

    private Product product(String name, String slug, String categoryName, String description) {
        Category category = new Category();
        category.setName(categoryName);
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setDescription(description);
        product.setPrice(BigDecimal.valueOf(50_000));
        product.setStock(10);
        product.setStatus(ProductStatus.IN_STOCK);
        product.setUnit("kg");
        return product;
    }
}
