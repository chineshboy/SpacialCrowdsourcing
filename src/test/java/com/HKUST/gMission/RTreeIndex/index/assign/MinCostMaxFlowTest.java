package com.HKUST.gMission.RTreeIndex.index.assign;

import org.junit.Before;
import org.junit.Test;

import com.HKUST.gMission.SpacialCroudsourcing.assign.MinCostMaxFlow;

public class MinCostMaxFlowTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void test() {
	MinCostMaxFlow flow = new MinCostMaxFlow();
	int cap[][] = {{0, 3, 4, 5, 0},
                {0, 0, 2, 0, 0},
                {0, 0, 0, 4, 1},
                {0, 0, 0, 0, 10},
                {0, 0, 0, 0, 0}};

	double cost1[][] = {{0, 1, 0, 0, 0},
                  {0, 0, 0, 0, 0},
                  {0, 0, 0, 0, 0},
                  {0, 0, 0, 0, 0},
                  {0, 0, 0, 0, 0}};

	double cost2[][] = {{0, 0, 1, 0, 0},
                  {0, 0, 0, 0, 0},
                  {0, 0, 0, 0, 0},
                  {0, 0, 0, 0, 0},
                  {0, 0, 0, 0, 0}};
 
	// should print out:
        //   10 1
        //   10 3
       
        double ret1[] = flow.getMaxFlow(cap, cost1, 0, 4);
        double ret2[] = flow.getMaxFlow(cap, cost2, 0, 4);
        
        System.out.println (ret1[0] + " " + ret1[1]);
        System.out.println (ret2[0] + " " + ret2[1]);
    }

}
