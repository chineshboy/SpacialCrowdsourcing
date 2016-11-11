package com.HKUST.gMission.RTreeIndex.index.rtree;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.HKUST.gMission.SpacialCroudsourcing.Point;

import gnu.trove.procedure.TIntProcedure;
import net.sf.jsi.Rectangle;

public class RTreeTest {
    
    net.sf.jsi.SpatialIndex index;
    com.HKUST.gMission.SpacialCroudsourcing.SpatialIndex newIndex;

    @Before
    public void setUp() throws Exception {
	newIndex = new com.HKUST.gMission.SpacialCroudsourcing.rtree.RTree();
	newIndex.init(null);
	index = new net.sf.jsi.rtree.RTree();
	index.init(null);
    }

    @Test
    public void test() {
	int pNum = 10000;
	int k = 1000;
	for (int i = 0; i < pNum; i++) {
	    List<Float> coo = randomPos();
	    PutNode(coo.get(0), coo.get(1), i);
	    PutNodeNew(coo.get(0), coo.get(1), i);
	}
	List<Float> coo = randomPos();
	List<Integer> truth = NearestK(coo.get(0), coo.get(1), k, Float.POSITIVE_INFINITY);
	List<Integer> newly = NearestKNew(coo.get(0), coo.get(1), k, Float.POSITIVE_INFINITY);
	
	assert(truth.size() == newly.size());
	//System.out.println(truth.toString() + '\n' + newly.toString());
	
	for (int i = 0; i < newly.size(); i++) {
	    if (truth.get(i) != newly.get(i)) {
		
		assert(truth.get(i).equals(newly.get(i)));
	    }
	}
    }
    
    private List<Float> randomPos() {
	List<Float> list = new ArrayList<Float>();
	list.add((float)Math.random()*100);
	list.add((float)Math.random()*100);
	return list;
    }
    
    private void PutNode(float longitude, float latitude, int id) {
	// if the node already exists, delete it first, then add the new
	// position.
	Rectangle newRect = new Rectangle(longitude, latitude, longitude, latitude);
	index.add(newRect, id);
    }
    
    private void PutNodeNew(double longitude, double latitude, int id) {
	List<Double> coo = new ArrayList<Double>();
	coo.add(longitude);
	coo.add(latitude);
	com.HKUST.gMission.SpacialCroudsourcing.Rectangle r = new com.HKUST.gMission.SpacialCroudsourcing.Rectangle(coo, coo, 2);
	newIndex.add(r, id);
    }
    
    private List<Integer> NearestK(float longitude, float latitude, int k, float furthest) {
	net.sf.jsi.Point p = new net.sf.jsi.Point(longitude, latitude);
	SaveToListProcedure proc = new SaveToListProcedure();
	index.nearestN(p, proc, k, furthest);
	return proc.getIds();
    }
    
    private List<Integer> NearestKNew(double longitude, double latitude, int k, double furthest) {
	List<Double> coo = new ArrayList<Double>();
	coo.add(longitude);
	coo.add(latitude);
	Point p = new Point(coo);
	return newIndex.nearestN(p, k, furthest);
    }

    class SaveToListProcedure implements TIntProcedure {
	private List<Integer> ids = new ArrayList<Integer>();

	public boolean execute(int id) {
	    ids.add(id);
	    return true;
	};

	private List<Integer> getIds() {
	    return ids;
	}
    };
}
