/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmt.engine.NetStrategies.ForkStrategies;

import jmt.engine.QueueNet.JobClass;

/**
 *
 * @author mattia
 */
public class ClassJobNum {
        
        private String name;
        private Integer[] numbers;
        private String[] classes;
        
        public ClassJobNum(String name, String[] classes, Integer[] numbers) {
                this.name = name;
                this.numbers = numbers;
                this.classes = classes;
        }
        
        public ClassJobNum(String name, String[] classes, String[] numbers) {
                this.name = name;
                this.numbers = new Integer[numbers.length];
                for(int i = 0; i < numbers.length; i++)
                        this.numbers[i] = Integer.parseInt(numbers[i]);
                this.classes = classes;
        }

        /**
         * @return the name
         */
        public String getName() {
                return name;
        }

        /**
         * @param name the name to set
         */
        public void setName(String name) {
                this.name = name;
        }

        /**
         * @return the numbers
         */
        public Integer[] getNumbers() {
                return numbers.clone();
        }

        /**
         * @return the classes
         */
        public String[] getClasses() {
                return classes.clone();
        }
        
}
