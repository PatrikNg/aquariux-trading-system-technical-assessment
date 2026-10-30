package com.aquariux.technical.assessment.trade.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import com.aquariux.technical.assessment.trade.entity.CryptoPair;

@Mapper
public interface CryptoPairMapper {
    
    @Select("""
            SELECT id FROM crypto_pairs 
            WHERE pair_name = #{pairName}
            """)
    Long findIdByPairName(@Param("pairName") String pairName);

    @Select("""
        SELECT id, base_symbol_id, quote_symbol_id, pair_name, active
        FROM crypto_pairs
        WHERE pair_name = #{pairName} AND active = TRUE
        """)
    CryptoPair findActiveByPairName(@Param("pairName")String pairName);
}