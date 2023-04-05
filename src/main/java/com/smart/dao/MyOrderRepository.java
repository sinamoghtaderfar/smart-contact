package com.smart.dao;

import com.smart.entities.Myorder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MyOrderRepository extends JpaRepository<Myorder,Long> {

    public Myorder findByOrderId(String orderId);

}
