package com.HKUST.gMission.SpacialCroudsourcing.assign.module;

import com.HKUST.gMission.SpacialCroudsourcing.Point;
import com.HKUST.gMission.SpacialCroudsourcing.Rectangle;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GeneralWorker {
    public int id;

    public double latitude;

    public double longitude;

    public int capacity;

    public int assigned;

    private double activeness;

    public Rectangle region;

    private double minDirection;
    private double maxDirection;
    private double velocity;
    public double reliability;

    public Point location;

    public int getCapacity() {
        return this.capacity - this.assigned;
    }

    public static List<GeneralWorker> queryActiveWorkersFromDB(JdbcTemplate jdbcTemplate) {
        String sql = "select U.id as id, longitude, latitude, capacity, region_min_lon, region_max_lon, region_min_lat, region_max_lat, reliability"
                + " from gmission_hkust.user U join gmission_hkust.user_last_position P join gmission_hkust.worker_detail D"
                + " on P.user_id = U.id and D.id = U.id"
                + " where is_online=1";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

        List<GeneralWorker> workers = new ArrayList<GeneralWorker>();
        for (Map<String, Object> row : result) {
            GeneralWorker w = new GeneralWorker();
            w.id = (Integer) row.get("id");
            w.longitude = (Double) row.get("longitude");
            w.latitude = (Double) row.get("latitude");
            w.location = new Point(w.longitude, w.latitude);
            w.region = new Rectangle((Double) row.get("region_min_lon"), (Double) row.get("region_min_lat"),
                    (Double) row.get("region_max_lon"), (Double) row.get("region_max_lat"));
            w.capacity = (Integer) row.get("capacity");
            w.reliability = (Double) row.get("reliability");

            String sqlAssigned = "select count(*) from gmission_hkust.message" +
                    " where att_type = 'HIT' and receiver_id = " + w.id;
            List<Map<String, Object>> r = jdbcTemplate.queryForList(sqlAssigned);
            w.assigned = ((Long) r.get(0).get("count(*)")).intValue();
            w.capacity -= w.assigned;
            workers.add(w);
        }

        return workers;
    }

    public static List<List<GeneralWorker>> queryAllWorkersFromDB(JdbcTemplate jdbcTemplate) {
        String sql = "select id, longitude, latitude, capacity, region_min_lon, region_max_lon, region_min_lat," +
                     " region_max_lat, reliability, is_online" +
                     " from gmission_hkust.worker_detail" +
                     " order by is_online";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

        List<List<GeneralWorker>> instances = new ArrayList<List<GeneralWorker>>();
        for (Map<String, Object> row : result) {
            GeneralWorker w = new GeneralWorker();
            w.id = (Integer) row.get("id");
            w.longitude = (Double) row.get("longitude");
            w.latitude = (Double) row.get("latitude");
            w.location = new Point(w.longitude, w.latitude);
            w.region = new Rectangle((Double) row.get("region_min_lon"), (Double) row.get("region_min_lat"),
                    (Double) row.get("region_max_lon"), (Double) row.get("region_max_lat"));
            w.capacity = (Integer) row.get("capacity");
            w.reliability = (Double) row.get("reliability");

            int is_online = (Integer) row.get("is_online");
            while (is_online >= instances.size()) {
                instances.add(new ArrayList<GeneralWorker>());
            }
            instances.get(is_online).add(w);
        }

        return instances;
    }
}
