package com.vatti.chzscout.backend.ai.infrastructure.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VectorTypeTest {

  VectorType vectorType;

  @Mock ResultSet resultSet;
  @Mock PreparedStatement preparedStatement;
  @Mock SharedSessionContractImplementor session;

  @BeforeEach
  void setUp() {
    vectorType = new VectorType();
  }

  @Nested
  @DisplayName("nullSafeGet 메서드 테스트")
  class NullSafeGet {

    @Test
    @DisplayName("pgvector 문자열을 float 배열로 변환한다")
    void parsesVectorStringToFloatArray() throws SQLException {
      // given
      given(resultSet.getString(1)).willReturn("[1.0,2.0,3.0]");
      given(resultSet.wasNull()).willReturn(false);

      // when
      float[] result = vectorType.nullSafeGet(resultSet, 1, session, null);

      // then
      assertThat(result).containsExactly(1.0f, 2.0f, 3.0f);
    }

    @Test
    @DisplayName("null 값이면 null을 반환한다")
    void returnsNullForNullValue() throws SQLException {
      // given
      given(resultSet.getString(1)).willReturn(null);

      // when
      float[] result = vectorType.nullSafeGet(resultSet, 1, session, null);

      // then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("wasNull이 true면 null을 반환한다")
    void returnsNullWhenWasNull() throws SQLException {
      // given
      given(resultSet.getString(1)).willReturn("[1.0]");
      given(resultSet.wasNull()).willReturn(true);

      // when
      float[] result = vectorType.nullSafeGet(resultSet, 1, session, null);

      // then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("빈 벡터 문자열은 빈 배열로 변환한다")
    void parsesEmptyVectorToEmptyArray() throws SQLException {
      // given
      given(resultSet.getString(1)).willReturn("[]");
      given(resultSet.wasNull()).willReturn(false);

      // when
      float[] result = vectorType.nullSafeGet(resultSet, 1, session, null);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("공백이 포함된 벡터 문자열도 정상 파싱한다")
    void parsesVectorWithWhitespace() throws SQLException {
      // given
      given(resultSet.getString(1)).willReturn("[1.0, 2.0, 3.0]");
      given(resultSet.wasNull()).willReturn(false);

      // when
      float[] result = vectorType.nullSafeGet(resultSet, 1, session, null);

      // then
      assertThat(result).containsExactly(1.0f, 2.0f, 3.0f);
    }

    @Test
    @DisplayName("고차원 벡터도 정상 파싱한다")
    void parsesHighDimensionalVector() throws SQLException {
      // given - 1536차원 벡터 시뮬레이션 (10개만 테스트)
      StringBuilder vectorStr = new StringBuilder("[");
      for (int i = 0; i < 10; i++) {
        if (i > 0) vectorStr.append(",");
        vectorStr.append(i * 0.1f);
      }
      vectorStr.append("]");
      given(resultSet.getString(1)).willReturn(vectorStr.toString());
      given(resultSet.wasNull()).willReturn(false);

      // when
      float[] result = vectorType.nullSafeGet(resultSet, 1, session, null);

      // then
      assertThat(result).hasSize(10);
      assertThat(result[0]).isEqualTo(0.0f);
      assertThat(result[9]).isCloseTo(0.9f, org.assertj.core.data.Offset.offset(0.001f));
    }
  }

  @Nested
  @DisplayName("nullSafeSet 메서드 테스트")
  class NullSafeSet {

    @Test
    @DisplayName("float 배열을 pgvector 문자열로 변환하여 설정한다")
    void formatsFloatArrayToVectorString() throws SQLException {
      // given
      float[] vector = {1.0f, 2.0f, 3.0f};

      // when
      vectorType.nullSafeSet(preparedStatement, vector, 1, session);

      // then
      verify(preparedStatement).setObject(1, "[1.0,2.0,3.0]", Types.OTHER);
    }

    @Test
    @DisplayName("null 값이면 setNull을 호출한다")
    void setsNullForNullValue() throws SQLException {
      // when
      vectorType.nullSafeSet(preparedStatement, null, 1, session);

      // then
      verify(preparedStatement).setNull(1, Types.OTHER);
    }

    @Test
    @DisplayName("빈 배열은 빈 벡터 문자열로 변환한다")
    void formatsEmptyArrayToEmptyVector() throws SQLException {
      // given
      float[] vector = {};

      // when
      vectorType.nullSafeSet(preparedStatement, vector, 1, session);

      // then
      verify(preparedStatement).setObject(1, "[]", Types.OTHER);
    }
  }

  @Nested
  @DisplayName("equals 메서드 테스트")
  class Equals {

    @Test
    @DisplayName("같은 내용의 배열은 동등하다")
    void equalArraysAreEqual() {
      // given
      float[] x = {1.0f, 2.0f, 3.0f};
      float[] y = {1.0f, 2.0f, 3.0f};

      // when & then
      assertThat(vectorType.equals(x, y)).isTrue();
    }

    @Test
    @DisplayName("다른 내용의 배열은 동등하지 않다")
    void differentArraysAreNotEqual() {
      // given
      float[] x = {1.0f, 2.0f, 3.0f};
      float[] y = {1.0f, 2.0f, 4.0f};

      // when & then
      assertThat(vectorType.equals(x, y)).isFalse();
    }

    @Test
    @DisplayName("null과 null은 동등하다")
    void nullEqualsNull() {
      // when & then
      assertThat(vectorType.equals(null, null)).isTrue();
    }

    @Test
    @DisplayName("null과 배열은 동등하지 않다")
    void nullNotEqualsArray() {
      // given
      float[] x = {1.0f};

      // when & then
      assertThat(vectorType.equals(null, x)).isFalse();
      assertThat(vectorType.equals(x, null)).isFalse();
    }
  }

  @Nested
  @DisplayName("deepCopy 메서드 테스트")
  class DeepCopy {

    @Test
    @DisplayName("배열의 독립적인 복사본을 생성한다")
    void createsIndependentCopy() {
      // given
      float[] original = {1.0f, 2.0f, 3.0f};

      // when
      float[] copy = vectorType.deepCopy(original);
      original[0] = 999.0f; // 원본 수정

      // then
      assertThat(copy[0]).isEqualTo(1.0f); // 복사본은 영향받지 않음
      assertThat(copy).containsExactly(1.0f, 2.0f, 3.0f);
    }

    @Test
    @DisplayName("null은 null을 반환한다")
    void returnsNullForNull() {
      // when & then
      assertThat(vectorType.deepCopy(null)).isNull();
    }
  }

  @Nested
  @DisplayName("기타 메서드 테스트")
  class OtherMethods {

    @Test
    @DisplayName("SQL 타입은 OTHER이다")
    void sqlTypeIsOther() {
      assertThat(vectorType.getSqlType()).isEqualTo(Types.OTHER);
    }

    @Test
    @DisplayName("반환 클래스는 float[]이다")
    void returnedClassIsFloatArray() {
      assertThat(vectorType.returnedClass()).isEqualTo(float[].class);
    }

    @Test
    @DisplayName("타입은 mutable이다")
    void typeIsMutable() {
      assertThat(vectorType.isMutable()).isTrue();
    }

    @Test
    @DisplayName("hashCode는 배열 내용 기반이다")
    void hashCodeIsContentBased() {
      // given
      float[] x = {1.0f, 2.0f, 3.0f};
      float[] y = {1.0f, 2.0f, 3.0f};

      // when & then
      assertThat(vectorType.hashCode(x)).isEqualTo(vectorType.hashCode(y));
    }

    @Test
    @DisplayName("disassemble은 deepCopy와 동일하게 동작한다")
    void disassembleReturnsCopy() {
      // given
      float[] original = {1.0f, 2.0f};

      // when
      float[] result = (float[]) vectorType.disassemble(original);
      original[0] = 999.0f;

      // then
      assertThat(result[0]).isEqualTo(1.0f);
    }

    @Test
    @DisplayName("assemble은 deepCopy와 동일하게 동작한다")
    void assembleReturnsCopy() {
      // given
      float[] cached = {1.0f, 2.0f};

      // when
      float[] result = vectorType.assemble(cached, null);
      cached[0] = 999.0f;

      // then
      assertThat(result[0]).isEqualTo(1.0f);
    }
  }
}
