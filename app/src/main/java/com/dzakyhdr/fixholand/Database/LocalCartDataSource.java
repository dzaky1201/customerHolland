package com.dzakyhdr.fixholand.Database;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

public class LocalCartDataSource implements CartDataSource {

    private CartDAO cartDAO;

    public LocalCartDataSource(CartDAO cartDAO) {
        this.cartDAO = cartDAO;
    }

    @Override
    public Flowable<List<CartItem>> getAllCart(String uid) {
        return cartDAO.getAllCart(uid);
    }

    @Override
    public Single<Integer> countItemInCart(String uid) {
        return cartDAO.countItemInCart(uid);
    }

    @Override
    public Single<Double> sumPriceInCart(String uid) {
        return cartDAO.sumPriceInCart(uid);
    }

    @Override
    public Single<CartItem> getItemInCart(String foodId, String uid) {
        return cartDAO.getItemInCart(foodId,uid);
    }

    @Override
    public Completable insertOrReplaceAll(CartItem... cartItems) {
        return cartDAO.insertOrReplaceAll(cartItems);
    }

    @Override
    public Single<Integer> updateCartItems(CartItem cartItem) {
        return cartDAO.updateCartItem(cartItem);
    }

    @Override
    public Single<Integer> deleteCartItems(CartItem cartItem) {
        return cartDAO.deleteCartItem(cartItem);
    }

    @Override
    public Single<Integer> clearCart(String uid) {
        return cartDAO.cleanCart(uid);
    }

    @Override
    public Single<CartItem> getItemAllOptionsInCart(String uid, String foodId, String foodSize, String foodAddon) {
        return cartDAO.getItemWithAllOptionsInCart(uid,foodId,foodSize,foodAddon);
    }
}
