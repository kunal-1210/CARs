package com.example.cars;

import android.os.Bundle;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class Mybookings extends AppCompatActivity {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ViewPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mybookings);

        tabLayout = findViewById(R.id.tabLayout_roles);
        viewPager = findViewById(R.id.viewPager_roles);

        adapter = new ViewPagerAdapter(this);

        // Add fragments
        adapter.addFragment(new MyTripsFragment());    // My Trips tab
        adapter.addFragment(new MyListingFragment()); // Host/My Listings tab

        viewPager.setAdapter(adapter);

        // Attach TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                if (position == 0) tab.setText("My Trips");
                else tab.setText("My Listings");
            }).attach();
    }
}