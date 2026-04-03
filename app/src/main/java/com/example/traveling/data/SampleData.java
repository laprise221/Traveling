package com.example.traveling.data;

import com.example.traveling.R;
import com.example.traveling.model.Photo;
import com.example.traveling.model.TravelPath;

import java.util.ArrayList;
import java.util.List;

public class SampleData {

    public static List<Photo> getSamplePhotos() {
        List<Photo> photos = new ArrayList<>();

        photos.add(new Photo("p1", "Tour Eiffel au coucher du soleil",
                "Vue magnifique de la Tour Eiffel avec les lumières dorées du crépuscule",
                "Marie Dupont", "Paris, France",
                48.8584, 2.2945, "15 mars 2026", "monument", 245,
                R.drawable.sample_photo_1));

        photos.add(new Photo("p2", "Plage de Bali",
                "Sable blanc et eaux turquoises à Nusa Dua",
                "Thomas Martin", "Bali, Indonésie",
                -8.7980, 115.1674, "22 janvier 2026", "nature", 189,
                R.drawable.sample_photo_2));

        photos.add(new Photo("p3", "Ruelle de Kyoto",
                "Promenade dans le quartier de Gion avec les maisons traditionnelles",
                "Sophie Chen", "Kyoto, Japon",
                35.0037, 135.7786, "8 avril 2026", "rue", 312,
                R.drawable.sample_photo_3));

        photos.add(new Photo("p4", "Marché de Marrakech",
                "Les couleurs et épices du souk de la médina",
                "Ahmed Benali", "Marrakech, Maroc",
                31.6295, -7.9811, "5 février 2026", "magasin", 156,
                R.drawable.sample_photo_4));

        photos.add(new Photo("p5", "Colisée de Rome",
                "L'amphithéâtre antique sous le soleil d'été",
                "Luca Rossi", "Rome, Italie",
                41.8902, 12.4922, "12 juin 2025", "monument", 478,
                R.drawable.sample_photo_5));

        photos.add(new Photo("p6", "Fjords de Norvège",
                "Paysage spectaculaire des fjords depuis le sommet",
                "Erik Hansen", "Geirangerfjord, Norvège",
                62.1048, 7.0940, "20 juillet 2025", "nature", 523,
                R.drawable.sample_photo_6));

        return photos;
    }

    public static List<TravelPath> getSamplePaths() {
        List<TravelPath> paths = new ArrayList<>();

        paths.add(new TravelPath("t1", "Paris en un jour",
                "Paris", "Découvrez les incontournables de Paris en une journée",
                "Guide Traveling", 48.8566, 2.3522,
                "8h", "€€", "Modéré", "équilibré", 6, 342,
                R.drawable.sample_path_1));

        paths.add(new TravelPath("t2", "Rome antique",
                "Rome", "Parcours à travers les vestiges de l'Empire romain",
                "Marco Travels", 41.9028, 12.4964,
                "5h", "€", "Facile", "économique", 4, 215,
                R.drawable.sample_path_2));

        paths.add(new TravelPath("t3", "Tokyo moderne & traditionnel",
                "Tokyo", "Entre temples ancestraux et quartiers futuristes",
                "Yuki Explorer", 35.6762, 139.6503,
                "10h", "€€€", "Modéré", "confort", 8, 567,
                R.drawable.sample_path_3));

        paths.add(new TravelPath("t4", "Marrakech authentique",
                "Marrakech", "Immersion dans la culture marocaine : souks, palais et jardins",
                "Fatima Guide", 31.6295, -7.9811,
                "6h", "€", "Facile", "économique", 5, 198,
                R.drawable.sample_path_4));

        paths.add(new TravelPath("t5", "Barcelone artistique",
                "Barcelone", "Sur les traces de Gaudí et de l'art catalan",
                "Pablo Tours", 41.3851, 2.1734,
                "7h", "€€", "Modéré", "équilibré", 7, 431,
                R.drawable.sample_path_5));

        return paths;
    }
}
