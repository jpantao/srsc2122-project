package common;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Nonces {
    public static Map<Integer, Date> nonces = new HashMap<>();


    public static boolean isNonceNew(int na1Prime) {
        if (nonces.containsKey(na1Prime))
            return false;
        else {
            nonces.put(na1Prime, new Date());
            return true;
        }
    }
}
