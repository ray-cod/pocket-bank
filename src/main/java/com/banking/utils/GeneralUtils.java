package com.banking.utils;

import java.util.concurrent.ThreadLocalRandom;

public class GeneralUtils {

    public static String generateAccountNumber(){
        StringBuilder genNum = new StringBuilder();
        int rndNumber;

        for (int i = 0; i < 10; i++){
            rndNumber = ThreadLocalRandom.current().nextInt(0, 9);
            genNum.append(rndNumber);
        }
        return genNum.toString();
    }
}
