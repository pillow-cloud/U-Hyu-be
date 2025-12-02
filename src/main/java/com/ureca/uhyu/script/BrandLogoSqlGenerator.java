package com.ureca.uhyu.script;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BrandLogoSqlGenerator implements CommandLineRunner {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.s3.endpoint}")
    private String endpoint;

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    private static final String FOLDER_PREFIX = "logo/";
    private static final String[] EXTENSIONS = {"png", "jpg", "jpeg"};
    private static final String OUTPUT_FILE = "logo_update.sql";

    @Override
    public void run(String... args) {
        log.info("BrandLogoSqlGenerator 시작...");
        log.info("Endpoint: {}", endpoint);
        log.info("Bucket: {}", bucketName);

        // S3Client 생성 (Minio 지원)
        S3Client s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .endpointOverride(URI.create(endpoint))
                .forcePathStyle(true)
                .build();

        List<S3Object> objects;

        try {
            log.info("S3 객체 목록 조회 시도...");
            objects = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(FOLDER_PREFIX)
                    .build()).contents();
            log.info("S3 객체 목록 조회 성공. 개수: {}", objects.size());
        } catch (Exception e) {
            log.error("S3 객체 목록 조회 중 오류 발생", e);
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE, StandardCharsets.UTF_8))) {
            for (S3Object obj : objects) {
                String key = obj.key(); // 예: logo/스타벅스.png

                String fileName = key.replaceFirst(FOLDER_PREFIX, "");
                int dotIdx = fileName.lastIndexOf(".");
                if (dotIdx == -1) continue;

                String brandNameRaw = fileName.substring(0, dotIdx);
                String ext = fileName.substring(dotIdx + 1).toLowerCase();

                boolean valid = false;
                for (String e : EXTENSIONS) {
                    if (e.equals(ext)) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) continue;

                // NFC 정규화 (완성형 보장)
                String brandName = Normalizer.normalize(brandNameRaw, Normalizer.Form.NFC);

                String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8)
                        .replace("+", "%20")
                        .replace("%2F", "/");

                // Minio URL 생성 (endpoint + bucket + key)
                // endpoint가 http://localhost:9000 이라면 -> http://localhost:9000/bucket/key
                String imageUrl;
                if (endpoint.contains("amazonaws.com")) {
                     imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, encodedKey);
                } else {
                     imageUrl = String.format("%s/%s/%s", endpoint, bucketName, encodedKey);
                }

                String sql = String.format("UPDATE brands SET logo_image = '%s' WHERE brand_name = '%s';", imageUrl, brandName);
                writer.write(sql);
                writer.newLine();
            }
            writer.flush();
            log.info("SQL 파일 생성 완료: {}", OUTPUT_FILE);
        } catch (IOException e) {
            log.error("SQL 파일 저장 중 오류 발생", e);
        }
    }
}
