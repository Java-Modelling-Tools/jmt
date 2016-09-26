/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmt.engine.NetStrategies.JoinStrategies;

import jmt.engine.NetStrategies.JoinStrategy;

/**
 *
 * @author mattia
 */
public class GuardJoin extends JoinStrategy{
        
        private Integer[] required;
        private int req;
        
        public GuardJoin(String[] classes, String[] numbers) {
                required = new Integer[numbers.length];
                req = 0;
                for (int i = 0; i < numbers.length; i++) {
                        required[i] = Integer.parseInt(numbers[i]);
                        req += Integer.parseInt(numbers[i]);
                }
                
        }

        @Override
        public int getRequiredNum() {
                return req;
        }
        
        public Integer[] getRequiredPerClass() {
                return required.clone();
        }
        
}
