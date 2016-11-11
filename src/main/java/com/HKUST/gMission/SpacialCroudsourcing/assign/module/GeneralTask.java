package com.HKUST.gMission.SpacialCroudsourcing.assign.module;

import com.HKUST.gMission.SpacialCroudsourcing.Point;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GeneralTask {
    public static Logger logger = LogManager.getLogger();
    public int id;
    public double latitude;
    public double longitude;
    public long arrivalTime;
    public long expiryTime;
    public int requiredAnswerCount;
    public int assignedCount = 0;
    // the entropy of the location of this task
    // from GeoCrowd
    public double entropy = 0.5;
    public double confidence;

    public Point location;

    public int getRequirement() {
        return this.requiredAnswerCount - this.assignedCount;
    }

    public static List<GeneralTask> queryOpenTasksFromDB(JdbcTemplate jdbcTemplate, long currentTime) {
        String sql = "select H.id as id, required_answer_count, UNIX_TIMESTAMP(begin_time) as begin_time," +
                " UNIX_TIMESTAMP(end_time) as end_time, longitude, latitude, entropy, confidence, is_valid"
                + " from gmission_hkust.hit H join gmission_hkust.location L join gmission_hkust.coordinate C join gmission_hkust.hit_detail D"
                + " on H.location_id = L.id and L.coordinate_id = C.id and D.id=H.id"
                + " where is_valid=1 and UNIX_TIMESTAMP(begin_time) <= " + currentTime + " and UNIX_TIMESTAMP(end_time) >= " + currentTime;
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

        List<GeneralTask> tasks = new ArrayList<GeneralTask>();
        for (Map<String, Object> row : result) {
            GeneralTask t = new GeneralTask();
            t.id = (Integer) row.get("id");
            t.requiredAnswerCount = (Integer) row.get("required_answer_count");
            t.arrivalTime = (Long) row.get("begin_time");
            t.expiryTime = (Long) row.get("end_time");
            t.longitude = (Double) row.get("longitude");
            t.latitude = (Double) row.get("latitude");
            t.entropy = (Double) row.get("entropy");
            t.confidence = (Double) row.get("confidence");
            // get assignedCount count
            String tempSql = "select count(*) from gmission_hkust.message "
                    + "where att_type = 'HIT' and attachment = " + t.id;
            List<Map<String, Object>> tempResult = jdbcTemplate.queryForList(tempSql);
            t.assignedCount = ((Long) tempResult.get(0).get("count(*)")).intValue();

            t.location = new Point(t.longitude, t.latitude);
            tasks.add(t);
        }

        return tasks;
    }

    public static List<List<GeneralTask>> queryAllTasksFromDB(JdbcTemplate jdbcTemplate) {
        String sql = "select id, required_answer_count, begin_time," +
                " end_time, longitude, latitude, entropy, confidence, is_valid"
                + " from gmission_hkust.hit_detail";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

        List<List<GeneralTask>> instances = new ArrayList<List<GeneralTask>>();
        for (Map<String, Object> row : result) {
            GeneralTask t = new GeneralTask();
            t.id = (Integer) row.get("id");
            t.requiredAnswerCount = (Integer) row.get("required_answer_count");
            t.arrivalTime = (Integer) row.get("begin_time");
            t.expiryTime = (Integer) row.get("end_time");
            t.longitude = (Double) row.get("longitude");
            t.latitude = (Double) row.get("latitude");
            t.entropy = (Double) row.get("entropy");
            t.confidence = (Double) row.get("confidence");
            t.location = new Point(t.longitude, t.latitude);
            int is_valid = (Integer) row.get("is_valid");
            while (is_valid >= instances.size()) {
                instances.add(new ArrayList<GeneralTask>());
            }
            instances.get(is_valid).add(t);
        }
        for (int i = 1; i < instances.size(); i++) {
            for (GeneralTask t : instances.get(i - 1)) {
                if (t.expiryTime >= i) {
                    instances.get(i).add(t);
                }
            }
        }

        return instances;
    }
}
