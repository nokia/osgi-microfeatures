// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.storages.jdbc;

/**
 * @author marynows
 *
 */
public class InQueryBuilder {

    private InQueryBuilder() {
    }

    public static String buildQuery(final String column, final int count, final int limit) {
        final StringBuilder sql = new StringBuilder();
        if (count == 1) {
            createInQuery(sql, column, 1);
        } else if (count > 1) {
            final int times = count / limit;
            final int left = count % limit;
            for (int i = 0; i < times; i++) {
                createInQuery(sql, column, limit).append(" OR ");
            }
            if (left > 0) {
                createInQuery(sql, column, left);
            } else {
                sql.delete(sql.length() - 4, sql.length());
            }
        }

        if (sql.length() > 0) {
            return sql.insert(0, "(").append(")").toString();
        } else {
            return "";
        }
    }

    private static StringBuilder createInQuery(final StringBuilder sql, final String column, final int count) {
        sql.append(column);
        if (count == 1) {
            sql.append(" = ?");
        } else {
            sql.append(" IN (");
            for (int i = 0; i < count; i++) {
                sql.append("?,");
            }
            sql.delete(sql.length() - 1, sql.length()).append(")");
        }
        return sql;
    }
}
