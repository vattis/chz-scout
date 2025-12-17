package com.vatti.chzscout.backend.stream.fixture;

import com.vatti.chzscout.backend.stream.domain.AllFieldLiveDto;
import com.vatti.chzscout.backend.stream.domain.ChzzkLiveResponse;
import java.util.ArrayList;
import java.util.List;

/** ChzzkLiveResponse 테스트 픽스처. */
public class ChzzkLiveResponseFixture {

  private ChzzkLiveResponseFixture() {}

  /** 성공 응답 생성 (기본 20개 라이브 데이터). */
  public static ChzzkLiveResponse success() {
    return success(20);
  }

  /** 성공 응답 생성 (지정된 개수의 라이브 데이터). */
  public static ChzzkLiveResponse success(int liveCount) {
    return success(liveCount, null);
  }

  /** 성공 응답 생성 (다음 페이지 커서 포함). */
  public static ChzzkLiveResponse success(int liveCount, String nextCursor) {
    List<AllFieldLiveDto> data = createLiveDataList(liveCount);
    ChzzkLiveResponse.Page page = new ChzzkLiveResponse.Page(nextCursor);
    ChzzkLiveResponse.Content content = new ChzzkLiveResponse.Content(data, page);
    return new ChzzkLiveResponse(200, "OK", content);
  }

  /** 빈 데이터 응답 생성. */
  public static ChzzkLiveResponse empty() {
    ChzzkLiveResponse.Page page = new ChzzkLiveResponse.Page(null);
    ChzzkLiveResponse.Content content = new ChzzkLiveResponse.Content(List.of(), page);
    return new ChzzkLiveResponse(200, "OK", content);
  }

  /** 에러 응답 생성. */
  public static ChzzkLiveResponse error(int code, String message) {
    return new ChzzkLiveResponse(code, message, null);
  }

  /** 마지막 페이지 응답 생성 (nextCursor가 null). */
  public static ChzzkLiveResponse lastPage(int liveCount) {
    return success(liveCount, null);
  }

  /** 다음 페이지가 있는 응답 생성. */
  public static ChzzkLiveResponse withNextPage(int liveCount, String nextCursor) {
    return success(liveCount, nextCursor);
  }

  /** 페이지네이션 테스트용 다중 페이지 응답 리스트 생성. */
  public static List<ChzzkLiveResponse> paginatedResponses(int pageCount, int livePerPage) {
    List<ChzzkLiveResponse> responses = new ArrayList<>();
    for (int i = 0; i < pageCount; i++) {
      String nextCursor = (i < pageCount - 1) ? "cursor_" + (i + 1) : null;
      responses.add(success(livePerPage, nextCursor));
    }
    return responses;
  }

  private static List<AllFieldLiveDto> createLiveDataList(int count) {
    List<AllFieldLiveDto> list = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      list.add(AllFieldLiveDtoFixture.create(i));
    }
    return list;
  }
}
