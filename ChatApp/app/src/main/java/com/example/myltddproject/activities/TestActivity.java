package com.example.myltddproject.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.myltddproject.databinding.ActivityTestBinding;
import com.example.myltddproject.databinding.ActivityUsersBinding;

public class TestActivity extends AppCompatActivity {
ActivityTestBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(binding.getRoot());
    }
}