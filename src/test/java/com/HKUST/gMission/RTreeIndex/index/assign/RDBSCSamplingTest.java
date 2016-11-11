package com.HKUST.gMission.RTreeIndex.index.assign;

import com.HKUST.gMission.SpacialCroudsourcing.assign.RDB_SC_Sampling;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

public class RDBSCSamplingTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void test() {
        BigDecimal total = new BigDecimal("1000");
        String epsilon = "0.9";
        String alpha = "0.00001";
        BigInteger k = RDB_SC_Sampling.calculateK(total, epsilon, alpha);

        System.out.println("k = " + k);
    }

}
