package com.HKUST.gMission.SpacialCroudsourcing.assign.module;

import java.util.List;

public class SQLUtil {

    public static <T> String buildInClause(String sql, List<T> list) {
        StringBuffer result = new StringBuffer(sql);
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                result.append("(").append(list.get(i).toString());
            } else {
                result.append(",").append(list.get(i).toString());
            }
        }
        result.append(")");
        return result.toString();
    }

}
