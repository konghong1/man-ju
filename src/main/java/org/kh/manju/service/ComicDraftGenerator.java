package org.kh.manju.service;

import org.kh.manju.config.ManJuProperties;
import org.kh.manju.model.CreateProjectRequest;
import org.kh.manju.model.Episode;
import org.kh.manju.model.Panel;
import org.kh.manju.model.Scene;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Component
public class ComicDraftGenerator {

    private static final List<String> CAMERAS = List.of(
            "wide shot",
            "medium shot",
            "close-up",
            "over-shoulder shot",
            "low angle shot"
    );

    private static final List<String> SFX = List.of(
            "Boom",
            "Tap",
            "Whoosh",
            "Clang",
            "Crack"
    );

    private static final List<String> SCENE_LOCATIONS = List.of(
            "旧城区天台",
            "地下轨道站",
            "夜市后巷",
            "废弃实验室",
            "雨中的高架桥"
    );

    private final ManJuProperties properties;

    public ComicDraftGenerator(ManJuProperties properties) {
        this.properties = properties;
    }

    public String buildSynopsis(CreateProjectRequest request) {
        String language = normalizeLanguage(request.language());
        if ("en".equalsIgnoreCase(language) || "en-US".equalsIgnoreCase(language)) {
            return "%s must confront %s while pursuing %s in a %s-%s world."
                    .formatted(request.protagonist(), request.conflict(), request.premise(), request.genre(), request.tone());
        }
        return "%s为了达成“%s”，必须正面迎战“%s”，故事基调为%s，面向%s。"
                .formatted(request.protagonist(), request.premise(), request.conflict(), request.tone(), request.targetAudience());
    }

    public List<Episode> buildEpisodes(CreateProjectRequest request) {
        int sceneCount = request.episodeLength().sceneCount();
        int panelsPerScene = Math.max(2, properties.getPanelsPerScene());
        Random random = new Random(Objects.hash(request.title(), request.protagonist(), request.conflict()));

        List<Scene> scenes = new ArrayList<>();
        for (int sceneIndex = 1; sceneIndex <= sceneCount; sceneIndex++) {
            String location = SCENE_LOCATIONS.get((sceneIndex - 1) % SCENE_LOCATIONS.size());
            String goal = "推进主角目标：" + request.premise() + "（阶段 " + sceneIndex + "/" + sceneCount + "）";
            String conflictBeat = "冲突升级：" + request.conflict() + "，压力值 " + (sceneIndex * 15) + "%";
            scenes.add(new Scene(
                    sceneIndex,
                    location,
                    goal,
                    conflictBeat,
                    buildPanels(request, sceneIndex, panelsPerScene, random)
            ));
        }

        Episode episode = new Episode(
                1,
                request.title(),
                "围绕“%s”展开的一集，逐步升级至关键对抗。".formatted(request.conflict()),
                scenes
        );
        return List.of(episode);
    }

    private List<Panel> buildPanels(
            CreateProjectRequest request,
            int sceneIndex,
            int panelsPerScene,
            Random random
    ) {
        List<Panel> panels = new ArrayList<>();
        for (int panelIndex = 1; panelIndex <= panelsPerScene; panelIndex++) {
            String camera = CAMERAS.get(random.nextInt(CAMERAS.size()));
            String sfx = SFX.get(random.nextInt(SFX.size()));
            String narration = "第%s幕第%s格：%s进入关键动作，现场气氛趋紧。"
                    .formatted(sceneIndex, panelIndex, request.protagonist());
            String dialogue = "“这一步，必须赢下来。”";
            String imagePrompt = buildImagePrompt(request, sceneIndex, panelIndex, camera);
            panels.add(new Panel(panelIndex, camera, narration, dialogue, sfx, imagePrompt));
        }
        return panels;
    }

    private String buildImagePrompt(CreateProjectRequest request, int sceneIndex, int panelIndex, String camera) {
        return "comic panel, %s style, %s tone, scene %d panel %d, %s, protagonist %s, conflict %s, cinematic lighting"
                .formatted(
                        request.visualStyle(),
                        request.tone(),
                        sceneIndex,
                        panelIndex,
                        camera,
                        request.protagonist(),
                        request.conflict()
                );
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "zh-CN";
        }
        return language.trim();
    }
}
