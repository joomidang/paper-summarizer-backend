package joomidang.papersummary.common.util;

import java.util.Collection;
import java.util.List;

/**
 * 벡터 연산을 위한 유틸리티 클래스
 */
public class VectorUtils {

    /**
     * 여러 벡터의 평균 벡터 계산
     *
     * @param vectors 벡터 목록
     * @return 평균 벡터
     */
    public static float[] average(List<float[]> vectors) {
        if (vectors == null || vectors.isEmpty()) {
            return new float[0];
        }

        int dim = vectors.get(0).length;
        float[] avg = new float[dim];

        for (float[] vec : vectors) {
            for (int i = 0; i < dim; i++) {
                avg[i] += vec[i];
            }
        }

        for (int i = 0; i < dim; i++) {
            avg[i] /= vectors.size();
        }

        return avg;
    }

    /**
     * 다양한 타입을 float[]로 안전하게 변환 (개선된 버전)
     *
     * @param input 입력 객체 (List<Float>, List<Number>, float[] 등)
     * @return float 배열
     */
    public static float[] toFloatArray(Object input) {
        if (input == null) {
            return new float[0];
        }

        // 이미 float[] 타입인 경우
        if (input instanceof float[]) {
            return (float[]) input;
        }

        // Collection 타입인 경우 (List, ArrayList 등)
        if (input instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) input;
            if (collection.isEmpty()) {
                return new float[0];
            }

            float[] result = new float[collection.size()];
            int index = 0;

            for (Object value : collection) {
                result[index++] = convertToFloat(value);
            }
            return result;
        }

        // 배열 타입인 경우
        if (input.getClass().isArray()) {
            if (input instanceof double[]) {
                double[] doubleArray = (double[]) input;
                float[] result = new float[doubleArray.length];
                for (int i = 0; i < doubleArray.length; i++) {
                    result[i] = (float) doubleArray[i];
                }
                return result;
            }

            if (input instanceof int[]) {
                int[] intArray = (int[]) input;
                float[] result = new float[intArray.length];
                for (int i = 0; i < intArray.length; i++) {
                    result[i] = (float) intArray[i];
                }
                return result;
            }
        }

        throw new IllegalArgumentException("지원하지 않는 입력 타입: " + input.getClass().getName() +
                ", 실제 값: " + input);
    }

    /**
     * 개별 값을 float으로 변환하는 헬퍼 메서드
     */
    private static float convertToFloat(Object value) {
        if (value == null) {
            return 0.0f;
        }

        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }

        if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("문자열을 float으로 변환할 수 없습니다: " + value, e);
            }
        }

        // 중첩된 컬렉션이나 다른 타입의 경우
        if (value instanceof Collection<?>) {
            throw new IllegalArgumentException("중첩된 컬렉션은 지원하지 않습니다. 값: " + value);
        }

        throw new IllegalArgumentException("float으로 변환할 수 없는 타입입니다. " +
                "타입: " + value.getClass().getName() + ", 값: " + value);
    }

    /**
     * 기존 메서드 유지 (하위 호환성)
     */
    @Deprecated
    public static float[] toFloatArray(List<?> list) {
        return toFloatArray((Object) list);
    }
}