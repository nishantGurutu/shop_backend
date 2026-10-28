package com.kisandaily.auth;
import jakarta.persistence.*;
import java.util.*;
@Entity
@Table(name="farmer_users", uniqueConstraints=@UniqueConstraint(name="uk_farmer_user_id", columnNames="user_id"))
class FarmerUser {
    @Id UUID id = UUID.randomUUID();
    @Column(name="user_id", nullable=false, length=30) String userId;
    @Column(nullable=false, length=100) String passwordHash;
    @Column(nullable=false, length=80) String name;
    @Column(length=16) String phone = "";
    @Column(length=100) String village = "";
    @Column(length=100) String district = "";
    @Column(length=100) String state = "मध्य प्रदेश";
    @Column(length=40) String language = "हिंदी (Hindi)";
    @Column(length=40) String landArea = "";
    @Column(nullable=false) boolean profileCompleted = false;
    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="farmer_crops", joinColumns=@JoinColumn(name="farmer_id"))
    @Column(name="crop", length=80)
    List<String> selectedCrops = new ArrayList<>();
    protected FarmerUser() {}
}
