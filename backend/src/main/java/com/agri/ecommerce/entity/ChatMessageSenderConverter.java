package com.agri.ecommerce.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ChatMessageSenderConverter implements AttributeConverter<ChatMessageSender, String> {
    @Override public String convertToDatabaseColumn(ChatMessageSender value) { return value == ChatMessageSender.ASSISTANT ? "bot" : "user"; }
    @Override public ChatMessageSender convertToEntityAttribute(String value) { return "bot".equalsIgnoreCase(value) ? ChatMessageSender.ASSISTANT : ChatMessageSender.USER; }
}
