package com.investlens.ingestion.application;

import java.util.List;

public interface NewsSourcePort {
    List<CollectedNews> collect();
}
