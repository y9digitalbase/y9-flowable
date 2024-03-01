package net.risesoft.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.io.Serializable;
import java.util.Date;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.GenericGenerator;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author qinman
 * @author zhangchongjie
 * @date 2022/12/20
 */
@NoArgsConstructor
@Data
@Entity
@Table(name = "FF_PRINTTEMPLATE")
@Comment("打印模板信息表")
public class PrintTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Comment("主键")
    @Column(name = "ID")
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "assigned")
    private String id;

    /**
     * 租户Id
     */
    @Comment("租户Id")
    @Column(name = "TENANTID", length = 50)
    private String tenantId;

    /**
     * 文档名称
     */
    @Comment("文档名称")
    @Column(name = "FILENAME", length = 50)
    private String fileName;

    /**
     * 文档路径
     */
    @Comment("文档路径")
    @Column(name = "FILEPATH", length = 2000)
    private String filePath;

    /**
     * 文件字节数
     */
    @Comment("文件大小")
    @Column(name = "FILESIZE", length = 20)
    private String fileSize;

    /**
     * 上传时间
     */
    @Comment("上传时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "UPLOADTIME")
    private Date uploadTime;

    /**
     * 上传人Id
     */
    @Comment("上传人Id")
    @Column(name = "PERSONID", length = 50)
    private String personId;

    /**
     * 上传人
     */
    @Comment("上传人")
    @Column(name = "PERSONNAME", length = 100)
    private String personName;

    /**
     * 文件描述
     */
    @Comment("文件描述")
    @Column(name = "DESCRIBES", length = 1000)
    private String describes;
}