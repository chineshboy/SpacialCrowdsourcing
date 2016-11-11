package com.HKUST.gMission.SpacialCroudsourcing.assign.module;

import com.HKUST.gMission.SpacialCroudsourcing.Rectangle;

public class RegionWorker extends GeneralWorker {

    public Rectangle region;
    /*
    public static List<RegionWorker> queryActiveWorkersFromDB(JdbcTemplate jdbcTemplate) {
        String sql = "select U.id as id, longitude, latitude, capacity, region_min_lon, region_max_lon, region_min_lat, region_max_lat "
                + "from gmission_hkust.user U join gmission_hkust.user_last_position P join gmission_hkust.worker_detail D "
                + "on P.user_id = U.id and D.id = U.id "
                + "where is_online=1";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

        List<RegionWorker> workers = new ArrayList<RegionWorker>();
        for (Map<String, Object> row : result) {
            RegionWorker w = new RegionWorker();
            w.id = (Integer) row.get("id");
            w.longitude = (Double) row.get("longitude");
            w.latitude = (Double) row.get("latitude");
            w.region = new Rectangle((Double) row.get("region_min_lon"), (Double) row.get("region_min_lat"),
                    (Double) row.get("region_max_lon"), (Double) row.get("region_max_lat"));
            w.capacity = (Integer) row.get("capacity");
            workers.add(w);
        }

        return workers;
    }
    */
}
