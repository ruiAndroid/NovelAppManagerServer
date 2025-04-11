import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.fun.novel.mapper.weiju", sqlSessionFactoryRef = "weijuSqlSessionFactory")
public class NovelWeijuDeliverSqlSessionConfig {

    @Bean("weijuSqlSessionFactory")
    public SqlSessionFactory weijuSqlSessionFactory(@Qualifier("dataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        
        MybatisConfiguration configuration = new MybatisConfiguration();
        // 关闭此SqlSessionFactory的驼峰命名转换
        configuration.setMapUnderscoreToCamelCase(false);
        factoryBean.setConfiguration(configuration);
        
        return factoryBean.getObject();
    }
    
    @Bean("weijuSqlSessionTemplate")
    public SqlSessionTemplate weijuSqlSessionTemplate(@Qualifier("weijuSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
} 