package com.dzakyhdr.fixholand.Database;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

public interface CartDataSource {
    Flowable<List<CartItem>> getAllCart(String uid);

    Single<Integer> countItemInCart(String uid);

    Single<Double> sumPriceInCart(String uid);

    Single<CartItem> getItemInCart(String foodId, String uid);

    Completable insertOrReplaceAll(CartItem... cartItems);

    Single<Integer> updateCartItems(CartItem cartItem);

    Single<Integer> deleteCartItems(CartItem cartItem);

    Single<Integer> clearCart(String uid);

    Single<CartItem> getItemAllOptionsInCart(String uid, String foodId, String foodSize, String foodAddon);
}
