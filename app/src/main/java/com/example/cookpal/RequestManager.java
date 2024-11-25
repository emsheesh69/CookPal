    package com.example.cookpal;

    import android.content.Context;
    import android.util.Log;

    import com.example.cookpal.Listeners.IngredientSubstituteListener;
    import com.example.cookpal.Models.IngredientSubstitution;
    import com.example.cookpal.Models.RandomRecipeApiResponse;
    import com.example.cookpal.Models.RecipeDetailsResponse;
    import com.example.cookpal.Models.ComplexSearchApiResponse; // New model class for complex search
    import com.example.cookpal.listeners.RandomRecipeResponseListener;
    import com.example.cookpal.listeners.RecipeDetailsListener;
    import com.example.cookpal.listeners.ComplexSearchListener; // New listener for complex search

    import java.util.List;

    import retrofit2.Call;
    import retrofit2.Callback;
    import retrofit2.Response;
    import retrofit2.Retrofit;
    import retrofit2.converter.gson.GsonConverterFactory;
    import retrofit2.http.GET;
    import retrofit2.http.Path;
    import retrofit2.http.Query;

    public class RequestManager {
        Context context;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.spoonacular.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        public RequestManager(Context context) {
            this.context = context;
        }

        // Method to fetch random recipes without titleMatch
        public void getComplexSearch(ComplexSearchListener listener, List<String> excludeIngredients, List<String> includeIngredients, int number, String titleMatch) {
            CallComplexSearch callComplexSearch = retrofit.create(CallComplexSearch.class);

            String includeIngredientsString = String.join(",", includeIngredients);
            String excludeIngredientsString = String.join(",", excludeIngredients);


            Log.d("ComplexSearch", "Include Ingredients: " + includeIngredientsString);
            Log.d("ComplexSearch", "Exclude Ingredients: " + excludeIngredientsString);

            Call<ComplexSearchApiResponse> call = callComplexSearch.callComplexSearch(
                    context.getString(R.string.api_key),
                    number,
                    includeIngredientsString,
                    excludeIngredientsString,
                    titleMatch // Pass titleMatch as a parameter
            );

            call.enqueue(new Callback<ComplexSearchApiResponse>() {
                @Override
                public void onResponse(Call<ComplexSearchApiResponse> call, Response<ComplexSearchApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listener.didFetch(response.body(), response.message());
                    } else {
                        listener.didError(response.message());
                    }
                }

                @Override
                public void onFailure(Call<ComplexSearchApiResponse> call, Throwable t) {
                    listener.didError(t.getMessage() != null ? t.getMessage() : "An error occurred");
                }
            });
        }
        public void getRandomRecipes(RandomRecipeResponseListener listener, List<String> tags) {
            CallRandomRecipes callRandomRecipes = retrofit.create(CallRandomRecipes.class);
            Call<RandomRecipeApiResponse> call = callRandomRecipes.callRandomRecipe(context.getString(R.string.api_key), 50, tags);

            call.enqueue(new Callback<RandomRecipeApiResponse>() {
                @Override
                public void onResponse(Call<RandomRecipeApiResponse> call, Response<RandomRecipeApiResponse> response) {
                    if (!response.isSuccessful()) {
                        listener.didError(response.message());
                        return;
                    }
                    listener.didFetch(response.body(), response.message());
                }

                @Override
                public void onFailure(Call<RandomRecipeApiResponse> call, Throwable t) {
                    listener.didError(t.getMessage());
                }
            });
        }

        public void getIngredientSubstitute(String ingredientName, IngredientSubstituteListener listener) {
            CallIngredientSubstitution callIngredientSubstitution = retrofit.create(CallIngredientSubstitution.class);

            Call<IngredientSubstitution> call = callIngredientSubstitution.callIngredientSubstitution(
                    ingredientName,
                    context.getString(R.string.api_key)
            );

            call.enqueue(new Callback<IngredientSubstitution>() {
                @Override
                public void onResponse(Call<IngredientSubstitution> call, Response<IngredientSubstitution> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listener.didFetch(response.body(), "Success");
                    } else {
                        listener.didError("Error: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<IngredientSubstitution> call, Throwable t) {
                    listener.didError(t.getMessage() != null ? t.getMessage() : "An error occurred");
                }
            });
        }
        // Method to fetch recipe details by ID
        public void getRecipeDetails(RecipeDetailsListener listener, int id) {
            CallRecipeDetails callRecipeDetails = retrofit.create(CallRecipeDetails.class);
            Call<RecipeDetailsResponse> call = callRecipeDetails.callRecipeDetails(id, context.getString(R.string.api_key));

            call.enqueue(new Callback<RecipeDetailsResponse>() {
                @Override
                public void onResponse(Call<RecipeDetailsResponse> call, Response<RecipeDetailsResponse> response) {
                    if (response.isSuccessful()) {
                        listener.didFetch(response.body(), "Success");
                    } else {
                        listener.didError("Error: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<RecipeDetailsResponse> call, Throwable throwable) {
                    listener.didError(throwable.getMessage());
                }
            });
        }





        private interface CallIngredientSubstitution {
            @GET("food/ingredients/substitutes")
            Call<IngredientSubstitution> callIngredientSubstitution(
                    @Query("ingredientName") String ingredientName,
                    @Query("apiKey") String apiKey
            );
        }

        // Interface for random recipes API
        private interface CallRandomRecipes {
            @GET("recipes/random")
            Call<RandomRecipeApiResponse> callRandomRecipe(
                    @Query("apiKey") String apiKey,
                    @Query("number") int number,
                    @Query("tags") List<String> tags
            );
        }

        // Interface for recipe details API
        private interface CallRecipeDetails {
            @GET("recipes/{id}/information")
            Call<RecipeDetailsResponse> callRecipeDetails(
                    @Path("id") int id,
                    @Query("apiKey") String apiKey
            );
        }
        // Interface for complex search API
        private interface CallComplexSearch {
            @GET("recipes/complexSearch")
            Call<ComplexSearchApiResponse> callComplexSearch(
                    @Query("apiKey") String apiKey,
                    @Query("number") int number,
                    @Query("includeIngredients") String includeIngredients,
                    @Query("excludeIngredients") String excludeIngredients,
                    @Query("titleMatch") String titleMatch
            );
        }
    }
