package com.HKUST.gMission.SpacialCroudsourcing;

import com.HKUST.gMission.SpacialCroudsourcing.action.RequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

public class IndexLoader {

    public static Logger logger = LogManager.getLogger();

    public IndexLoader(JdbcTemplate jdbcTemplate, RequestHandler index, String enable) {
        if (enable.equals("yes")) {
            logger.info("[START LOADING INDEX FROM DB]");
            String sql = "select user_id, longitude, latitude from gmission_hkust.user_last_position";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : result) {
                int userID = (Integer) row.get("user_id");
                float longitude = ((Double) row.get("longitude")).floatValue();
                float latitude = ((Double) row.get("latitude")).floatValue();
                index.PutNode(longitude, latitude, userID);
            }
            logger.info("[FINISHED LOADING]");
        } else {
            logger.info("[LOADER DISABLED BY CONFIG][" + enable + "]");
        }
    }

}
