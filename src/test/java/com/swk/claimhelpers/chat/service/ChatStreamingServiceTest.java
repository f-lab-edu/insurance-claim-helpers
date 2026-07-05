package com.swk.claimhelpers.chat.service;

import com.swk.claimhelpers.chat.dto.ChatContext;
import com.swk.claimhelpers.chat.dto.PreparedChat;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatStreamingServiceTest {

    private ChatClient chatClientReturning(Flux<String> stream) {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().messages(anyList()).stream().content()).thenReturn(stream);
        return chatClient;
    }

    @Test
    void 정상_완료_시_ASSISTANT_저장() {
        ChatMessageService messageService = mock(ChatMessageService.class);
        ChatContext context = new ChatContext(List.of(10L), List.of());
        when(messageService.loadContext(1L, "질문", null, "sk")).thenReturn(context);
        when(messageService.prepareContext("질문", context))
                .thenReturn(new PreparedChat(List.<Message>of(), true));
        when(messageService.saveAssistant(eq(1L), anyString())).thenReturn(99L);
        ChatClient chatClient = chatClientReturning(Flux.just("a", "b"));

        ChatStreamingService service = new ChatStreamingService(messageService, chatClient);
        service.stream(1L, "질문", null, "sk");

        verify(messageService).saveAssistant(1L, "ab");
    }

    @Test
    void 스트림_에러_시_저장_안함() {
        ChatMessageService messageService = mock(ChatMessageService.class);
        ChatContext context = new ChatContext(List.of(10L), List.of());
        when(messageService.loadContext(1L, "질문", null, "sk")).thenReturn(context);
        when(messageService.prepareContext("질문", context))
                .thenReturn(new PreparedChat(List.<Message>of(), true));
        ChatClient chatClient = chatClientReturning(Flux.error(new RuntimeException("boom")));

        ChatStreamingService service = new ChatStreamingService(messageService, chatClient);
        service.stream(1L, "질문", null, "sk");

        verify(messageService, never()).saveAssistant(anyLong(), anyString());
    }

    @Test
    void 관련_청크_없으면_LLM_생략하고_고정_답변_저장() {
        ChatMessageService messageService = mock(ChatMessageService.class);
        ChatContext context = new ChatContext(List.of(10L), List.of());
        when(messageService.loadContext(1L, "질문", null, "sk")).thenReturn(context);
        when(messageService.prepareContext("질문", context))
                .thenReturn(new PreparedChat(List.<Message>of(), false));
        when(messageService.saveAssistant(eq(1L), anyString())).thenReturn(7L);
        
        ChatClient chatClient = mock(ChatClient.class);

        ChatStreamingService service = new ChatStreamingService(messageService, chatClient);
        service.stream(1L, "질문", null, "sk");

        verify(messageService).saveAssistant(eq(1L), contains("관련된 약관 조항을 찾지 못해"));
        verifyNoInteractions(chatClient);
    }

    @Test
    void 완료_처리_중_저장_실패하면_emitter_에러로_종료() {
        ChatMessageService messageService = mock(ChatMessageService.class);
        when(messageService.saveAssistant(1L, "ab")).thenThrow(new RuntimeException("db 실패"));
        ChatStreamingService service = new ChatStreamingService(messageService, mock(ChatClient.class));
        SseEmitter emitter = mock(SseEmitter.class);

        service.complete(emitter, 1L, "ab");

        verify(emitter).completeWithError(any(Throwable.class));
    }

    @Test
    void 전송_중_예외나면_emitter_에러로_종료() throws Exception {
        ChatStreamingService service =
                new ChatStreamingService(mock(ChatMessageService.class), mock(ChatClient.class));
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IllegalStateException("이미 닫힘")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        service.sendDelta(emitter, new StringBuilder(), "delta");

        verify(emitter).completeWithError(any(Throwable.class));
    }

    @Test
    void 타임아웃_콜백이_LLM_구독을_취소() {
        ChatStreamingService service =
                new ChatStreamingService(mock(ChatMessageService.class), mock(ChatClient.class));
        SseEmitter emitter = mock(SseEmitter.class);
        Disposable subscription = mock(Disposable.class);

        service.registerCleanup(emitter, subscription, 1L);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(emitter).onTimeout(captor.capture());
        captor.getValue().run();
        verify(subscription).dispose();
    }
}