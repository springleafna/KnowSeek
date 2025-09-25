package com.springleaf.knowseek.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.math.BigDecimal;
import java.sql.*;

@MappedTypes(float[].class)
@MappedJdbcTypes(JdbcType.OTHER)
public class VectorTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null || parameter.length == 0) {
            ps.setNull(i, Types.OTHER);
            return;
        }

        StringBuilder sb = new StringBuilder("[");
        for (int j = 0; j < parameter.length; j++) {
            if (j > 0) sb.append(",");
            float f = parameter[j];
            if (Float.isNaN(f) || Float.isInfinite(f)) {
                sb.append("0.0");
            } else {
                sb.append(BigDecimal.valueOf(f).stripTrailingZeros().toPlainString());
            }
        }
        sb.append("]");
        ps.setObject(i, sb.toString(), Types.OTHER);
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseVectorString(rs.getString(columnName));
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseVectorString(rs.getString(columnIndex));
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseVectorString(cs.getString(columnIndex));
    }

    public float[] parseVectorString(String vectorStr) throws SQLException {
        if (vectorStr == null || vectorStr.trim().isEmpty()) {
            return null;
        }

        try {
            String trimmed = vectorStr.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }
            if (trimmed.isEmpty()) return new float[0];

            String[] parts = trimmed.split(",");
            float[] result = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].trim();
                result[i] = part.isEmpty() ? 0.0f : Float.parseFloat(part);
            }
            return result;
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new SQLException("Failed to parse vector: " + vectorStr, e);
        }
    }
}