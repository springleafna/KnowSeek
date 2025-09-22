package com.springleaf.knowseek.service;

import java.util.List;

public interface EmbeddingService {

    List<float[]> embedTexts(List<String> texts);
}
