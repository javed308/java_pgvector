package com.javed.java_pgvector.helper;

import java.util.StringJoiner;

public class VectorCodec {

    private VectorCodec() {
    }

    public static void validate(float[] vector, int expectedDimensions) {
        if (expectedDimensions <= 0) {
            throw new IllegalArgumentException("Expected dimension must be positive");
        }
        if (vector == null || vector.length != expectedDimensions) {
            throw new IllegalArgumentException("Expected " + expectedDimensions
                    + " embedding values, received " + (vector == null ? "null" : vector.length));
        }
        boolean nonzero = false;
        for (float value : vector) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Embedding values must be finite");
            }
            nonzero |= value != 0.0f;
        }
        if (!nonzero) {
            throw new IllegalArgumentException("A zero vector cannot be used for cosine search");
        }
    }

    public static String toLiteral(float[] vector) {
        validate(vector, vector == null ? 1 : vector.length);
        StringJoiner values = new StringJoiner(",", "[", "]");
        for (float value : vector) {
            // Float.toString is independent of the computer's locale.
            values.add(Float.toString(value));
        }
        return values.toString();
    }
}
