package com.springleaf.knowseek.test;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.springleaf.knowseek.constans.UploadRedisKeyConstant;
import com.springleaf.knowseek.handler.VectorTypeHandler;
import com.springleaf.knowseek.mapper.mysql.FileUploadMapper;
import com.springleaf.knowseek.mapper.pgvector.VectorRecordMapper;
import com.springleaf.knowseek.model.entity.FileUpload;
import com.springleaf.knowseek.model.entity.VectorRecord;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ApiTest {

    @Resource
    private EmbeddingModel embeddingModel;
    @Resource
    private VectorRecordMapper vectorRecordMapper;
    @Resource
    private FileUploadMapper fileUploadMapper;

    @Test
    void testDeleteVectorRecord() {
        vectorRecordMapper.deleteByFileId(62L);
    }

    @Test
    void testGetFileName() {
        FileUpload file = fileUploadMapper.getFileById(52L);
        System.out.println("fileName:" + file.getFileName());
    }

    @Test
    void testVectorRoundTrip() throws SQLException {
        VectorTypeHandler handler = new VectorTypeHandler();

        // 测试写入格式
        float[] input = {0.1f, -0.000123f, 1e-5f, 0.0f, -0.0f, 1.0f};
        String expected = "[0.1,-0.000123,0.00001,0,0,1]";

        StringBuilder sb = new StringBuilder("[");
        for (int j = 0; j < input.length; j++) {
            if (j > 0) sb.append(",");
            float f = input[j];
            if (Float.isInfinite(f)) {
                sb.append("0.0");
            } else {
                sb.append(BigDecimal.valueOf(f).stripTrailingZeros().toPlainString());
            }
        }
        sb.append("]");
        assertEquals(expected, sb.toString());

        // 测试解析
        float[] output = handler.parseVectorString(expected);
        assertArrayEquals(new float[]{0.1f, -0.000123f, 1e-5f, 0.0f, 0.0f, 1.0f}, output, 0.00001f);
    }

    @Test
    public void testInsertVector() {
        VectorRecord vectorRecord = new VectorRecord();
        // 测试少量文本
        String texts = "hello world";
        float[] embeddings = embeddingModel.embed(texts);
        vectorRecord.setUserId(1001L);
        vectorRecord.setKnowledgeBaseId(2001L);
        vectorRecord.setOrganizationId(3001L);
        vectorRecord.setFileId(11111L);
        vectorRecord.setChunkIndex(1);
        vectorRecord.setChunkText(texts);
        vectorRecord.setEmbedding(embeddings);
        vectorRecord.setCreatedAt(LocalDateTime.now());
        vectorRecord.setUpdatedAt(LocalDateTime.now());

        // 执行插入
        int rows = vectorRecordMapper.insert(vectorRecord);

        // 验证插入成功
        System.out.println("插入影响行数: " + rows);


    }

    // 将 float[] 转为 pgvector 字符串格式: "[0.1,0.2,0.3,...]"
    private String vectorToString(float[] vector) {
        if (vector == null || vector.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    @Test
    public void testEmbeddingConnection() {
        try {
            // 测试少量文本
            List<String> texts = List.of("测试连接", "hello world", "spring ai");

            List<float[]> embeddings = embeddingModel.embed(texts);

            System.out.println("连接成功！");
            System.out.println("嵌入向量维度: " + embeddings.size());

        } catch (Exception e) {
            System.err.println("连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void test_getEnv() {
        String accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        String accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
        System.out.println(accessKeyId);
        System.out.println(accessKeySecret);
    }


    @Test
    public void test_redisKey() {
        String formatted = String.format(UploadRedisKeyConstant.FILE_UPLOAD_INIT_KEY, "123");
        System.out.println(formatted);
    }


    /**
     * 测试阿里云OSS文件上传
     */
    @Test
    public void test_oss_upload() throws com.aliyuncs.exceptions.ClientException {
        // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。
        String endpoint = "https://oss-cn-hangzhou.aliyuncs.com";
        // 从环境变量中获取访问凭证。运行本代码示例之前，请确保已设置环境变量OSS_ACCESS_KEY_ID和OSS_ACCESS_KEY_SECRET。
        EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
        // 填写Bucket名称，例如examplebucket。
        String bucketName = "know-seek-bucket";
        // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
        String objectName = "test/test.txt";
        // 填写本地文件的完整路径，例如D:\\localpath\\examplefile.txt。
        // 如果未指定本地路径，则默认从示例程序所属项目对应本地路径中上传文件。
        String filePath= "C:\\Users\\xxx\\Desktop\\新建 文本文档.txt";
        // 填写Bucket所在地域。以华东1（杭州）为例，Region填写为cn-hangzhou。
        String region = "cn-hangzhou";

        // 创建OSSClient实例。
        // 当OSSClient实例不再使用时，调用shutdown方法以释放资源。
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();

        try {
            // 创建PutObjectRequest对象。
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, new File(filePath));
            // 如果需要上传时设置存储类型和访问权限，请参考以下示例代码。
            // ObjectMetadata metadata = new ObjectMetadata();
            // metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
            // metadata.setObjectAcl(CannedAccessControlList.Private);
            // putObjectRequest.setMetadata(metadata);

            // 上传文件。
            PutObjectResult result = ossClient.putObject(putObjectRequest);
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
