(function () {
    const HERITAGE_DATA = [
        {
            id: 1,
            title: "The Great Wall of China",
            description: "Ancient defensive architecture spanning thousands of miles",
            type: "site",
            typeName: "Historical Site",
            category: "Historical Site",
            tags: "history,china,UNESCO",
            location: "beijing",
            locationName: "Beijing",
            background: "Built over centuries to protect China from invasions",
            contributors: "UNESCO",
            date: "2026-04-08",
            image: "🏛️",
            views: 0
        },
        {
            id: 2,
            title: "Terracotta Army",
            description: "Collection of terracotta sculptures depicting ancient armies",
            type: "artifact",
            typeName: "Artifact",
            category: "Artifact",
            tags: "qin dynasty,history",
            location: "xian",
            locationName: "Xi'an",
            background: "Buried with the first Emperor of China",
            contributors: "Archaeologists",
            date: "2026-04-07",
            image: "⚱️",
            views: 0
        },
        {
            id: 3,
            title: "Chinese Calligraphy",
            description: "Traditional art of writing Chinese characters with brush and ink",
            type: "art",
            typeName: "Art & Craft",
            category: "Art & Craft",
            tags: "culture,art",
            location: "hangzhou",
            locationName: "Hangzhou",
            background: "An important part of Chinese cultural heritage",
            contributors: "Artists",
            date: "2026-04-06",
            image: "🖌️",
            views: 0
        },
        {
            id: 4,
            title: "Dragon Boat Festival",
            description: "Traditional festival commemorating Qu Yuan",
            type: "tradition",
            typeName: "Tradition",
            category: "Tradition",
            tags: "festival,culture",
            location: "shanghai",
            locationName: "Shanghai",
            background: "Celebrated with dragon boat races and rice dumplings",
            contributors: "Community",
            date: "2026-04-05",
            image: "🐉",
            views: 0
        },
        {
            id: 5,
            title: "Forbidden City",
            description: "Imperial palace complex in Beijing",
            type: "building",
            typeName: "Architecture",
            category: "Architecture",
            tags: "palace,history",
            location: "beijing",
            locationName: "Beijing",
            background: "Home to emperors for over 500 years",
            contributors: "UNESCO",
            date: "2026-04-04",
            image: "🏯",
            views: 0
        },
        {
            id: 6,
            title: "Sichuan Opera",
            description: "Traditional opera with face-changing performance",
            type: "tradition",
            typeName: "Tradition",
            category: "Tradition",
            tags: "opera,performance",
            location: "chengdu",
            locationName: "Chengdu",
            background: "Famous for its dramatic face-changing technique",
            contributors: "Performers",
            date: "2026-04-03",
            image: "🎭",
            views: 0
        },
        {
            id: 7,
            title: "Peking Opera",
            description: "Traditional Chinese theatre art",
            type: "tradition",
            typeName: "Tradition",
            category: "Tradition",
            tags: "opera,theatre",
            location: "beijing",
            locationName: "Beijing",
            background: "Combines music, dance, and acting",
            contributors: "Artists",
            date: "2026-04-02",
            image: "🎭",
            views: 0
        },
        {
            id: 8,
            title: "Giant Panda Sanctuaries",
            description: "Natural habitat of giant pandas",
            type: "site",
            typeName: "Historical Site",
            category: "Historical Site",
            tags: "nature,animals",
            location: "chengdu",
            locationName: "Chengdu",
            background: "Protects endangered giant pandas",
            contributors: "Conservationists",
            date: "2026-04-01",
            image: "🐼",
            views: 0
        }
    ];

    function getHeritageData() {
        return HERITAGE_DATA.map(item => ({ ...item }));
    }

    function persistHeritageData(data) {
        localStorage.setItem("heritageData", JSON.stringify(data));
    }

    function ensureHeritageData() {
        const raw = localStorage.getItem("heritageData");
        if (!raw) {
            persistHeritageData(getHeritageData());
        }
    }

    function readHeritageDataFromStorage() {
        const raw = localStorage.getItem("heritageData");
        if (!raw) {
            return [];
        }

        try {
            return JSON.parse(raw);
        } catch (error) {
            return [];
        }
    }

    function normalizeLocationForFilter(locationValue) {
        return (locationValue || "").toString().trim().toLowerCase();
    }

    window.AppCommon = {
        getHeritageData: getHeritageData,
        persistHeritageData: persistHeritageData,
        ensureHeritageData: ensureHeritageData,
        readHeritageDataFromStorage: readHeritageDataFromStorage,
        normalizeLocationForFilter: normalizeLocationForFilter
    };
})();
