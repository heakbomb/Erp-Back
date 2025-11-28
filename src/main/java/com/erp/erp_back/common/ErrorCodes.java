package com.erp.erp_back.common;

public final class ErrorCodes {

    private ErrorCodes() {
    }

    public static final String STORE_NOT_FOUND = "STORE_NOT_FOUND";
    public static final String INVENTORY_NOT_FOUND = "INVENTORY_NOT_FOUND";
    public static final String INVENTORY_ITEM_NOT_FOUND = "INVENTORY_ITEM_NOT_FOUND";
    public static final String MENU_NOT_FOUND = "MENU_NOT_FOUND";
    public static final String PURCHASE_NOT_FOUND = "PURCHASE_NOT_FOUND";
    public static final String RECIPE_INGREDIENT_NOT_FOUND = "RECIPE_INGREDIENT_NOT_FOUND";

    public static final String STORE_ID_MUST_NOT_BE_NULL = "STORE_ID_MUST_NOT_BE_NULL";
    public static final String MENU_ID_MUST_NOT_BE_NULL = "MENU_ID_MUST_NOT_BE_NULL";
    public static final String RECIPE_ID_MUST_NOT_BE_NULL = "RECIPE_ID_MUST_NOT_BE_NULL";
    public static final String INVENTORY_ID_MUST_NOT_BE_NULL = "INVENTORY_ID_MUST_NOT_BE_NULL";
    public static final String DUPLICATE_MENU_NAME = "DUPLICATE_MENU_NAME";
    public static final String INGREDIENT_ALREADY_EXISTS_FOR_MENU = "INGREDIENT_ALREADY_EXISTS_FOR_MENU";

    public static final String INVALID_CONSUMPTION_QTY = "INVALID_CONSUMPTION_QTY";
    public static final String NEGATIVE_STOCK_NOT_ALLOWED = "NEGATIVE_STOCK_NOT_ALLOWED";

    public static final String STORE_MISMATCH_BETWEEN_MENU_AND_INVENTORY = "STORE_MISMATCH_BETWEEN_MENU_AND_INVENTORY";
    public static final String MENU_STORE_MISMATCH = "MENU_STORE_MISMATCH";
    public static final String ITEM_NOT_BELONG_TO_STORE = "ITEM_NOT_BELONG_TO_STORE";

    public static final String CANNOT_ATTACH_INGREDIENT_TO_INACTIVE_MENU = "CANNOT_ATTACH_INGREDIENT_TO_INACTIVE_MENU";
    public static final String CANNOT_MODIFY_RECIPE_OF_INACTIVE_MENU = "CANNOT_MODIFY_RECIPE_OF_INACTIVE_MENU";
    public static final String CANNOT_USE_INACTIVE_INVENTORY_IN_RECIPE = "CANNOT_USE_INACTIVE_INVENTORY_IN_RECIPE";

    public static final String UNSUPPORTED_PERIOD = "Unsupported period";
}
