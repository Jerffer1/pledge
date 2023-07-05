package cn.pledge.envconsole.book.service;

import cn.hutool.core.util.ObjectUtil;
import cn.pledge.envconsole.book.entity.*;
import cn.pledge.envconsole.book.mapper.*;
import cn.pledge.envconsole.book.model.enums.GainInterestType;
import cn.pledge.envconsole.book.model.enums.PledgeType;
import cn.pledge.envconsole.book.model.param.*;
import cn.pledge.envconsole.book.model.vo.*;
import cn.pledge.envconsole.common.enums.Code;
import cn.pledge.envconsole.common.exception.BizException;
import cn.pledge.envconsole.common.model.PageResult;
import cn.pledge.envconsole.common.utils.EthUtils;
import cn.pledge.envconsole.common.utils.UserUtils;
import com.alibaba.fastjson.JSON;

import io.swagger.annotations.ApiModelProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 89466
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PledgeService {
    @Autowired
    private ConfigurationMapper configurationMapper;
    @Autowired
    private PledgeRecordMapper pledgeRecordMapper;
    @Autowired
    private FlowRecordMapper flowRecordMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StatisticsMapper statisticsMapper;
    @Autowired
    private ExperienceGoldRecordMapper experienceGoldRecordMapper;
    @Autowired
    private WithdrawRecordMapper withdrawRecordMapper;

    @Value("${contractAddress}")
    private String contractAddress;
    @Value("${contractAddressBRC20}")
    private String contractAddressBRC20;
    @Value("${contractAddressTRC20}")
    private String contractAddressTRC20;

    public ApplyPledgeVO applyPledge() {
        ApplyPledgeVO applyPledgeVO = new ApplyPledgeVO();
        Configuration configuration = configurationMapper.selectByPrimaryKey(1);
        if (configuration==null){
            return applyPledgeVO;
        }
        List<ApplyPledgeVO.Period> periods = JSON.parseArray(configuration.getPeriodList(), ApplyPledgeVO.Period.class);
        applyPledgeVO.setMinimumQuantity(configuration.getMinimumQuantity());
        applyPledgeVO.setPeriodList(periods);
        return applyPledgeVO;
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitPledge(SubmitPledgeParam param) {
        UserSession currentUser = UserUtils.getCurrentUser();
        Configuration configuration = configurationMapper.selectByPrimaryKey(1);
        Double minimumQuantity = configuration.getMinimumQuantity();
        if (minimumQuantity.compareTo(param.getAmount())>0){
            throw new BizException(Code.MINIMUM_QUANTITY_LIMIT);
        }
        PledgeRecord pledgeRecord = new PledgeRecord();
        BeanUtils.copyProperties(param,pledgeRecord);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime stopTime = now.plusDays(Long.parseLong(param.getPeriod()));
        pledgeRecord.setCreateTime(now);
        pledgeRecord.setCurrencyType(param.getCurrencyType());
        pledgeRecord.setUserId(currentUser.getUserId());
        pledgeRecord.setUserAddress(currentUser.getUserAddress());
        pledgeRecord.setIsReward(true);
        pledgeRecord.setIsVirtual(false);
        pledgeRecord.setStopTime(stopTime);
        pledgeRecord.setStatus(PledgeType.PLEDGING.toString());
        pledgeRecord.setIncome(new Double(0));
        pledgeRecordMapper.insertSelective(pledgeRecord);
        Statistics statistics = statisticsMapper.selectOneByUserId(currentUser.getUserId());
        Double unwithdrawPledge = statistics.getUnwithdrawPledge();

        BigDecimal.valueOf(unwithdrawPledge).add(BigDecimal.valueOf(param.getAmount()));
        statistics.setUnwithdrawPledge(BigDecimal.valueOf(unwithdrawPledge).add(BigDecimal.valueOf(param.getAmount())).doubleValue());
        statistics.setTotalPledge(BigDecimal.valueOf(statistics.getTotalPledge()).add(BigDecimal.valueOf(param.getAmount())).doubleValue());
        statisticsMapper.updateByPrimaryKeySelective(statistics);
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitFlow(SubmitFlowParam param) {

        UserSession currentUser = UserUtils.getCurrentUser();
        Configuration configuration = configurationMapper.selectByPrimaryKey(1);

        BigInteger bigInteger = BigInteger.ZERO;
        if(currentUser.getCurrencyType().equals("erc20")){
            Web3j web3j = Web3j.build(new HttpService("https://mainnet.infura.io/v3/77c83ab9cfd746918a9b188a7692fa00"));

            bigInteger = EthUtils.balanceOfErc20(web3j, contractAddress, currentUser.getUserAddress());
        }
        if (currentUser.getCurrencyType().equals("trc20")){
            bigInteger = EthUtils.balanceOfTrc20(contractAddressTRC20, currentUser.getUserAddress());
        }
        if(currentUser.getCurrencyType().equals("brc20")){
            Web3j web3j = Web3j.build(new HttpService("https://bsc-dataseed1.defibit.io/"));

            bigInteger = EthUtils.balanceOfErc20(web3j, contractAddressBRC20, currentUser.getUserAddress()).divide(new BigInteger("1000000000000"));
        }

        List<PledgeGlobalConfigurationVO.FlowMining> flowMinings = JSON.parseArray(configuration.getFlowMiningList(), PledgeGlobalConfigurationVO.FlowMining.class);

        BigDecimal period = BigDecimal.ZERO;
        for (PledgeGlobalConfigurationVO.FlowMining flowMining : flowMinings) {

        if (new BigInteger(flowMining.getPeriodMin()).multiply(new BigInteger(String.valueOf(1000000))).compareTo(bigInteger)<=0 &&
                new BigInteger(flowMining.getPeriodMax()).multiply(new BigInteger(String.valueOf(1000000))).compareTo(bigInteger)>=0){
            period = new BigDecimal(flowMining.getProfit());
            break;
        }
    }


        FlowRecord flowRecord = new FlowRecord();

        BigDecimal amount = BigDecimal.valueOf(bigInteger.doubleValue()).divide(BigDecimal.valueOf(1000000));
        FlowRecord flowRecordHas = flowRecordMapper.selectFlowRecordByUserId(currentUser.getUserId());
        if (flowRecordHas!=null){
            return;
        }
        flowRecord.setAmount(amount.doubleValue());
        flowRecord.setCreateTime(LocalDateTime.now());

        flowRecord.setPeriod(period.toString());
        flowRecord.setTime(0);
        flowRecord.setUserId(currentUser.getUserId());
        flowRecord.setUserAddress(currentUser.getUserAddress());
        flowRecord.setCurrencyType(currentUser.getCurrencyType());
        flowRecordMapper.insert(flowRecord);
        User user = new User();
        user.setId(currentUser.getUserId());
        user.setHasFlow(true);
        userMapper.updateByPrimaryKeySelective(user);

    }

    public PageResult<PledgeRecordVO> pledgeRecordList(PledgeRecordParam param) {

        User user = userMapper.selectByPrimaryKey(param.getUserId());
        if (user==null){
            throw new BizException(Code.USER_NOT_EXIST);
        }
        List<PledgeRecord> pledgeRecordPage = pledgeRecordMapper.PledgeRecordByUserId((param.getPage()-1)*param.getSize(), param.getSize(),user.getId(),param.getPledgeHash());
        Integer total = pledgeRecordMapper.total(user.getId());
        LocalDateTime now = LocalDateTime.now();
        List<PledgeRecordVO> collect = pledgeRecordPage.stream().map(o -> {
            PledgeRecordVO pledgeRecordVO = new PledgeRecordVO();
            BeanUtils.copyProperties(o, pledgeRecordVO);
            pledgeRecordVO.setPledgeCreateTime(o.getCreateTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
            if (PledgeType.PLEDGING.toString().equals(o.getStatus())) {
                //质押停止时间小于现在的时间
                if (now.compareTo(o.getStopTime())>=0) {
                    o.setStatus(PledgeType.APPLY.toString());
                    pledgeRecordMapper.updateByPrimaryKeySelective(o);
                    pledgeRecordVO.setStatus(PledgeType.APPLY.toString());
                    if (o.getIsReward()) {
                        Double amount = o.getAmount();
                        String profit = o.getProfit();
                        Double income = o.getIncome();
                        BigDecimal reward = new BigDecimal(profit).multiply(BigDecimal.valueOf(amount)).multiply(new BigDecimal("0.01"));
                        o.setIncome(BigDecimal.valueOf(income).add(reward).doubleValue());
                        Statistics statistics = statisticsMapper.selectOneByUserId(o.getUserId());
                        if (statistics!=null) {
                            Double unreceivedPledgeReward = statistics.getUnreceivedPledgeReward();
                            Double totalPledgeReward = statistics.getTotalPledgeReward();
                            statistics.setTotalPledgeReward(BigDecimal.valueOf(totalPledgeReward).add(reward).doubleValue());
                            statistics.setUnreceivedPledgeReward(BigDecimal.valueOf(unreceivedPledgeReward).add(reward).doubleValue());
                            statisticsMapper.updateByPrimaryKeySelective(statistics);
                        }
                    }

                }
            }

            return pledgeRecordVO;
        }).collect(Collectors.toList());
        PageResult<PledgeRecordVO> pledgeRecordVOPageResult = new PageResult<>();
        pledgeRecordVOPageResult.setItems(collect);
        pledgeRecordVOPageResult.setTotal(total);
        return pledgeRecordVOPageResult;
    }

    public SystemMessageVO systemMessage() {
        SystemMessageVO systemMessageVO = new SystemMessageVO();
        Configuration configuration = configurationMapper.selectByPrimaryKey(1);
        systemMessageVO.setIsNotice(configuration.getIsNotice());
        systemMessageVO.setSystemMessage(configuration.getSystemMessage());
        return systemMessageVO;
    }

    public PageResult<ExperienceGoldRecordVO> experienceGoldRecordList(ExperienceGoldRecordParam param) {
        User user = userMapper.selectByPrimaryKey(param.getUserId());
        if (user==null){
            throw new BizException(Code.USER_NOT_EXIST);
        }
        List<ExperienceGoldRecord> list = experienceGoldRecordMapper.experienceGoldRecord((param.getPage()-1)*param.getSize(), param.getSize(),user.getId(),null);
        Integer total = experienceGoldRecordMapper.total(user.getId(),null);
        List<ExperienceGoldRecordVO> collect = list.stream().map(o -> {
            ExperienceGoldRecordVO experienceGoldRecordVO = new ExperienceGoldRecordVO();
            BeanUtils.copyProperties(o, experienceGoldRecordVO);
            experienceGoldRecordVO.setCreateTime(o.getCreateTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
            if (o.getConfigTime()!=null){
             experienceGoldRecordVO.setConfigTime(o.getConfigTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
            }
            return experienceGoldRecordVO;
        }).collect(Collectors.toList());
        PageResult<ExperienceGoldRecordVO> experienceGoldRecordVOPageResult = new PageResult<>();
        experienceGoldRecordVOPageResult.setItems(collect);
        experienceGoldRecordVOPageResult.setTotal(total);
        return experienceGoldRecordVOPageResult;
    }



    public PageResult<SubordinateVO> subordinateList(SubordinateParam param) {
        UserSession currentUser = UserUtils.getCurrentUser();

        List<User> userList = userMapper.subordinateList((param.getPage()-1)*param.getSize(), param.getSize(),currentUser.getUserId());
        Integer total = userMapper.subordinateNum(currentUser.getUserId());
        List<SubordinateVO> collect = userList.stream().map(o -> {
            SubordinateVO subordinateVO = new SubordinateVO();
            subordinateVO.setCreateTime(o.getCreateTime().toInstant(ZoneOffset.ofHours(9)).toEpochMilli());
            subordinateVO.setUserAddress(o.getUserAddress());
            subordinateVO.setCurrencyType(o.getCurrencyType());
            return subordinateVO;
        }).collect(Collectors.toList());
        PageResult<SubordinateVO> subordinateVOPageResult = new PageResult<>();
        subordinateVOPageResult.setTotal(total);
        subordinateVOPageResult.setItems(collect);
        return subordinateVOPageResult;
    }

    public Double subordinateRewards() {
        UserSession currentUser = UserUtils.getCurrentUser();
        Statistics statistics = statisticsMapper.selectOneByUserId(currentUser.getUserId());
        return statistics.getTotalExperienceReward();
    }

    public void gainInterest(GainInterestParam param) {

        UserSession currentUser = UserUtils.getCurrentUser();
        Integer userId = currentUser.getUserId();
        String userAddress = currentUser.getUserAddress();
        if (ObjectUtil.isNotEmpty(param.getUserId())){
            User user = userMapper.selectByPrimaryKey(param.getUserId());
            if (user==null){
                throw new BizException(Code.USER_NOT_EXIST);
            }
            userId = user.getId();
            userAddress = user.getUserAddress();
        }
        Statistics statistics = statisticsMapper.selectOneByUserId(userId);
        Double unreceivedExperienceReward = statistics.getUnreceivedExperienceReward();
        Double unreceivedFlowReward = statistics.getUnreceivedFlowReward();
        Double unreceivedPledgeReward = statistics.getUnreceivedPledgeReward();
        Double virtualUnreceivedExperienceReward = statistics.getVirtualUnreceivedExperienceReward();
        Double virtualUnreceivedFlowReward = statistics.getVirtualUnreceivedFlowReward();
        Double virtualUnreceivedPledgeReward = statistics.getVirtualUnreceivedPledgeReward();
        if (GainInterestType.experience.equals(param.getType())) {
        if (unreceivedExperienceReward+virtualUnreceivedExperienceReward<=0){
            throw new BizException(Code.NOT_REWARD);
        }
      if (unreceivedExperienceReward+virtualUnreceivedExperienceReward > 0) {
        WithdrawRecord withdrawRecordFlow = new WithdrawRecord();
        withdrawRecordFlow.setApplyTime(LocalDateTime.now());
        withdrawRecordFlow.setAmount(unreceivedExperienceReward);
        withdrawRecordFlow.setStatus(PledgeType.WITHDRAWING.toString());
        withdrawRecordFlow.setWithdrewType(GainInterestType.experience.toString());
        withdrawRecordFlow.setUserId(userId);
        withdrawRecordFlow.setUserAddress(userAddress);
        withdrawRecordFlow.setCurrencyType(statistics.getCurrencyType());
          withdrawRecordFlow.setVirtualAmount(virtualUnreceivedExperienceReward);
          statistics.setUnreceivedExperienceReward(new Double(0));
          statistics.setVirtualUnreceivedExperienceReward(new Double(0));
        withdrawRecordMapper.insertSelective(withdrawRecordFlow);

      }
        }
    if (GainInterestType.flow.equals(param.getType())) {
        if (unreceivedFlowReward+virtualUnreceivedFlowReward<=0){
            throw new BizException(Code.NOT_REWARD);
        }
      if (unreceivedFlowReward + virtualUnreceivedFlowReward> 0) {
        WithdrawRecord withdrawRecordFlow = new WithdrawRecord();
        withdrawRecordFlow.setApplyTime(LocalDateTime.now());
        withdrawRecordFlow.setAmount(unreceivedFlowReward);
        withdrawRecordFlow.setStatus(PledgeType.WITHDRAWING.toString());
        withdrawRecordFlow.setWithdrewType(GainInterestType.flow.toString());
        withdrawRecordFlow.setUserId(userId);
          withdrawRecordFlow.setCurrencyType(statistics.getCurrencyType());
        withdrawRecordFlow.setUserAddress(userAddress);
        withdrawRecordFlow.setVirtualAmount(virtualUnreceivedFlowReward);
        statistics.setVirtualUnreceivedFlowReward(new Double(0));
        statistics.setUnreceivedFlowReward(new Double(0));
        withdrawRecordMapper.insertSelective(withdrawRecordFlow);

      }
        }
    if (GainInterestType.pledge.equals(param.getType())) {
        if (unreceivedPledgeReward+virtualUnreceivedPledgeReward<=0){
            throw new BizException(Code.NOT_REWARD);
        }
      if (unreceivedPledgeReward + virtualUnreceivedPledgeReward> 0) {
        WithdrawRecord withdrawRecordFlow = new WithdrawRecord();
        withdrawRecordFlow.setApplyTime(LocalDateTime.now());
        withdrawRecordFlow.setAmount(unreceivedPledgeReward);
        withdrawRecordFlow.setStatus(PledgeType.WITHDRAWING.toString());
        withdrawRecordFlow.setWithdrewType(GainInterestType.pledge.toString());
        withdrawRecordFlow.setUserId(userId);
        withdrawRecordFlow.setCurrencyType(statistics.getCurrencyType());
        withdrawRecordFlow.setUserAddress(userAddress);
        withdrawRecordFlow.setVirtualAmount(virtualUnreceivedPledgeReward);
        statistics.setVirtualUnreceivedPledgeReward(new Double(0));
        statistics.setUnreceivedPledgeReward(new Double(0));
        withdrawRecordMapper.insertSelective(withdrawRecordFlow);

      }
        }
        statisticsMapper.updateByPrimaryKeySelective(statistics);

    }
    public void withdrawalPrincipal(WithdrawalPrincipalParam param) {
        PledgeRecord pledgeRecord = pledgeRecordMapper.selectByPrimaryKey(param.getId());

        if(pledgeRecord.getIsVirtual()){
            throw new BizException(Code.VIRTUAL_PLEDGE_CANNOT_BE_WITHDRAWN);
        }
        if (pledgeRecord.getStopTime().compareTo(LocalDateTime.now())>=0) {
            throw new BizException(Code.PLEDGE_PERIOD_IS_NOT_OVER);
        }

        if (PledgeType.APPLY.toString().equals(pledgeRecord.getStatus())
                || PledgeType.PLEDGING.toString().equals(pledgeRecord.getStatus())) {
            PledgeRecord record = new PledgeRecord();
            record.setId(param.getId());
            record.setStatus(PledgeType.WITHDRAWING.toString());
            pledgeRecordMapper.updateByPrimaryKeySelective(record);

            User user = userMapper.selectByPrimaryKey(pledgeRecord.getUserId());
            if (user==null){
                throw new BizException(Code.USER_NOT_EXIST);
            }
            Statistics statistics = statisticsMapper.selectOneByUserId(user.getId());
            WithdrawRecord withdrawRecordFlow = new WithdrawRecord();
            withdrawRecordFlow.setApplyTime(LocalDateTime.now());
            withdrawRecordFlow.setAmount(pledgeRecord.getAmount());
            withdrawRecordFlow.setStatus(PledgeType.WITHDRAWING.toString());
            withdrawRecordFlow.setWithdrewType(GainInterestType.pledgePrincipal.toString());
            withdrawRecordFlow.setUserId(user.getId());
            withdrawRecordFlow.setUserAddress(user.getUserAddress());
            withdrawRecordMapper.insertSelective(withdrawRecordFlow);
            statistics.setUnwithdrawPledge(BigDecimal.valueOf(statistics.getUnwithdrawPledge()).subtract(BigDecimal.valueOf(pledgeRecord.getAmount())).doubleValue());
            statisticsMapper.updateByPrimaryKeySelective(statistics);
        }
    }
    public PageResult<WithdrawRecordVO> withdrawRecordList(WithdrawRecordParam param) {
        User user = userMapper.selectByPrimaryKey(param.getUserId());
        if (user==null){
            throw new BizException(Code.USER_NOT_EXIST);
        }
       List<WithdrawRecord> withdrawRecordList = withdrawRecordMapper.selectWithdrawRecordList((param.getPage()-1)*param.getSize(), param.getSize(),user.getId(),null,param.getPlayHash());
       Integer total = withdrawRecordMapper.withdrawRecordTotal(user.getId(),null,param.getPlayHash());
        List<WithdrawRecordVO> collect = withdrawRecordList.stream().map(o -> {
            WithdrawRecordVO withdrawRecordVO = new WithdrawRecordVO();
            BeanUtils.copyProperties(o, withdrawRecordVO);
            withdrawRecordVO.setApplyTime(o.getApplyTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
            if (o.getPlayTime() != null) {
                withdrawRecordVO.setPlayTime(o.getPlayTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
            }
            return withdrawRecordVO;
        }).collect(Collectors.toList());
        PageResult<WithdrawRecordVO> pageResult = new PageResult<>();
        pageResult.setItems(collect);
        pageResult.setTotal(total);
        return pageResult;
    }


    public void isAir( String email) {
        UserSession currentUser = UserUtils.getCurrentUser();
        User user = userMapper.selectByPrimaryKey(currentUser.getUserId());
        if (user==null){
            throw new BizException(Code.USER_NOT_EXIST);
        }
        Configuration configuration = configurationMapper.selectByPrimaryKey(1);
        if (!configuration.getIsAirdrop()) {
            throw new BizException(Code.NOT_IS_AIR);
        }
        if (user.getHasEmail()) {
            return;
        }
        user.setHasEmail(true);
        user.setEmail(email);
        userMapper.updateByPrimaryKeySelective(user);
    }
}
