package com.eventhub.event_hub.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderRequestDto(
    @NotEmpty(message = "A lista de itens não pode estar vazia")
    @Valid 
    List<OrderItemRequestDto> items
) {}