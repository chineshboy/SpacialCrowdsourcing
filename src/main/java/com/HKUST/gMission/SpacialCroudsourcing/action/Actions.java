package com.HKUST.gMission.SpacialCroudsourcing.action;

import com.HKUST.gMission.SpacialCroudsourcing.Point;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.WTMatch;
import com.HKUST.gMission.SpacialCroudsourcing.model.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Controller("Actions")
@RequestMapping("/actions")
public class Actions {
    public static Logger logger = LogManager.getLogger();

    @Resource(name = "indexWrapper")
    private RequestHandler requestHandler;

    @RequestMapping(value = "/disk/write", method = RequestMethod.GET)
    @ResponseBody
    public String writeIntoDisk() {
        logger.info("[REQUEST][WRITE INTO DISK]");
        requestHandler.writeIntoDisk();
        logger.info("[RESPONSE][WRITE INTO DISK][success]");
        return "success";
    }

    /**
     * add a new node into the index, if the node already exists, it would be replaced.
     *
     * @param ID
     * @param longitude
     * @param latitude
     * @return returns 200 if nothing wrong
     */
    @RequestMapping(value = "/node", method = RequestMethod.POST)
    @ResponseBody
    public String changeNode(@RequestParam(value = "ID", required = true) int ID,
                             @RequestParam(value = "longitude", required = true) float longitude,
                             @RequestParam(value = "latitude", required = true) float latitude) {
        logger.info("[REQUEST][PUT NODE][ID:" + ID + "][longitude:" + longitude + "][latitude:" + latitude + "]");

        requestHandler.PutNode(longitude, latitude, ID);
        logger.info("[RESPONSE][PUT NODE][success]");
        return "success";
    }

    /**
     * delete an existing node.
     *
     * @param ID
     * @return returns 200 if nothing wrong.
     */
    @RequestMapping(value = "/node", method = RequestMethod.DELETE)
    @ResponseBody
    public String deleteNode(@RequestParam(value = "ID", required = true) int ID,
                             @RequestParam(value = "longitude", required = true) float longitude,
                             @RequestParam(value = "latitude", required = true) float latitude) {
        logger.info("[REQUEST][DELETE NODE][ID:" + ID + "][longitude:" + longitude + "][latitude:" + latitude + "]");

        requestHandler.DeleteNode(longitude, latitude, ID);
        logger.info("[RESPONSE][DELETE NODE][success]");
        return "success";
    }

    /**
     * find the position of a node by its ID.
     *
     * @param ID
     * @return returns the ID, longitude, and latitude of the node.
     */
    @RequestMapping(value = "/node", method = RequestMethod.GET)
    @ResponseBody
    public Node getNode(@RequestParam(value = "ID", required = true) int ID) {
        logger.info("[REQUEST][GET NODE][ID:" + ID + "]");
        Node node = null;
        Point rect = requestHandler.getNode(ID);
        if (rect != null) {
            node = new Node();
            node.setID(ID);
            node.setLongitude(Math.round(rect.getLocation(1)));
            node.setLatitude(Math.round(rect.getLocation(2)));
        }
        logger.info("[RESPONSE][GET NODE][" + node + "]");
        return node;
    }

    /**
     * get all nodes in a specific rectangle.
     *
     * @param longitude1
     * @param latitude1
     * @param longitude2
     * @param latitude2
     * @return all nodes in the rectangle, including ID, longitude, latitude
     */
    @RequestMapping(value = "/nodes/rectangle", method = RequestMethod.GET)
    @ResponseBody
    public List<Integer> getNodesByRectangle(
            @RequestParam(value = "longitude1", required = true) float longitude1,
            @RequestParam(value = "latitude1", required = true) float latitude1,
            @RequestParam(value = "longitude2", required = true) float longitude2,
            @RequestParam(value = "latitude2", required = true) float latitude2) {
        logger.info("[REQUEST][GET NODES BY RECTANGLE][longitude1:" + longitude1 + "][latitude1:" + latitude1 + "][longitude2:" + longitude2
                + "][latitude2:" + latitude2 + "]");

        List<Integer> list = requestHandler.ContainsByRect(longitude1, latitude1, longitude2, latitude2);

        logger.info("[RESPONSE][GET NODES BY RECTANGLE][node count: " + list.size() + "]");
        return list;
    }

    /**
     * get all nodes in a specific rectangle by linear search.
     *
     * @param longitude1
     * @param latitude1
     * @param longitude2
     * @param latitude2
     * @return all nodes in the rectangle, including ID, longitude, latitude
     */
    @RequestMapping(value = "/nodes/rectangle/linear", method = RequestMethod.GET)
    @ResponseBody
    public List<Integer> getNodesByRectangleLinear(
            @RequestParam(value = "longitude1", required = true) float longitude1,
            @RequestParam(value = "latitude1", required = true) float latitude1,
            @RequestParam(value = "longitude2", required = true) float longitude2,
            @RequestParam(value = "latitude2", required = true) float latitude2) {
        logger.info("[REQUEST][GET NODES BY RECTANGLE LINEAR][longitude1:" + longitude1 + "][latitude1:" + latitude1 + "][longitude2:" + longitude2
                + "][latitude2:" + latitude2 + "]");

        List<Integer> list = requestHandler.LinearContainsByRect(longitude1, latitude1, longitude2, latitude2);

        logger.info("[RESPONSE][GET NODES BY RECTANGLE LINEAR][node count: " + list.size() + "]");
        return list;
    }

    /**
     * get k-nearest nodes.
     *
     * @param longitude
     * @param latitude
     * @param k
     * @return k nearest nodes, including ID, longitude, latitude
     */
    @RequestMapping(value = "/nodes/knn", method = RequestMethod.GET)
    @ResponseBody
    public List<Integer> getNodesByKNN(@RequestParam(value = "longitude", required = true) float longitude,
                                       @RequestParam(value = "latitude", required = true) float latitude,
                                       @RequestParam(value = "furthest", required = false) Float furthest,
                                       @RequestParam(value = "k", required = true) int k) {
        logger.info("[REQUEST][GET NODES BY KNN][longitude:" + longitude + "][latitude:" + latitude + "][k:" + k + "][furthest:" + furthest + "]");

        List<Integer> list;

        if (furthest == null) {
            furthest = Float.POSITIVE_INFINITY;
        }

        list = requestHandler.NearestK(longitude, latitude, k, furthest);

        logger.info("[RESPONSE][GET NODES BY KNN][node count: " + list.size() + "]");
        return list;
    }

    /**
     * get k-nearest nodes by linear search.
     *
     * @param longitude
     * @param latitude
     * @param k
     * @return k nearest nodes, including ID, longitude, latitude
     */
    @RequestMapping(value = "/nodes/knn/linear", method = RequestMethod.GET)
    @ResponseBody
    public List<Integer> getNodesByLinearKNN(@RequestParam(value = "longitude", required = true) float longitude,
                                             @RequestParam(value = "latitude", required = true) float latitude,
                                             @RequestParam(value = "furthest", required = false) Float furthest,
                                             @RequestParam(value = "k", required = true) int k) {
        logger.info("[REQUEST][GET NODES BY LINEAR KNN][longitude:" + longitude + "][latitude:" + latitude + "][k:" + k + "][furthest:" + furthest + "]");

        List<Integer> list;

        if (furthest == null) {
            furthest = Float.POSITIVE_INFINITY;
        }

        list = requestHandler.LinearNearestK(longitude, latitude, k, furthest);

        logger.info("[RESPONSE][GET NODES BY LINEAR KNN][node count: " + list.size() + "]");
        return list;
    }

    /**
     * assign tasks to workers
     *
     * @param method, the name of methods, one of "geocrowdgreedy", "geocrowdllep", "geocrowdnnp", "geotrucrowdgreedy",
     *                "geotrucrowdlo", "rdbscdivideandconquer", "rdbscsampling"
     * @param currentTime, current time represented by seconds from 1970 balabala
     * @return task and worker matches
     */
    @RequestMapping(value = "/assignment/{method}/{currentTime}", method = RequestMethod.POST)
    @ResponseBody
    public List<WTMatch> assignTasks(@PathVariable(value = "method") String method,
                                     @PathVariable(value = "currentTime") long currentTime) {
        logger.info("[REQUEST][ASSIGN TASKS][method: " + method + "][time:" + currentTime + "]");

        List<WTMatch> list = requestHandler.assignTasks(method, currentTime);

        logger.info("[RESPONSE][ASSIGN TASKS][method: " + method + "][time:" + currentTime + "]"
                + "[match count: " + list.size() + "]");
        return list;
    }

    /**
     * assign tasks to workers batch
     *
     * @param method, the name of methods, one of "geocrowdgreedy", "geocrowdllep", "geocrowdnnp", "geotrucrowdgreedy",
     *                "geotrucrowdlo", "rdbscdivideandconquer", "rdbscsampling"
     * @return task and worker matches
     */
    @RequestMapping(value = "/assignment/{method}/batch", method = RequestMethod.POST)
    @ResponseBody
    public List<WTMatch> assignTasksBatch(@PathVariable(value = "method") String method) {
        logger.info("[REQUEST][ASSIGN TASKS BATCH][method: " + method + "]");

        List<WTMatch> list = requestHandler.assignTasksBatch(method);

        logger.info("[RESPONSE][ASSIGN TASKS][method: " + method + "][match count: " + list.size() + "]");
        return list;
    }
}
