package com.example.cookpal.Models;

import java.util.ArrayList;
import java.util.List;

public class ComplexSearchApiResponse {
    public int offset;
    public int number;
    public int totalResults;
    public List<Recipe> results;


    public static class Recipe {
        public int id;
        public String title;
        public String image;
        public String imageType;
    }

}
