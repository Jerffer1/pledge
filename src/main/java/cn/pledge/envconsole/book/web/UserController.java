package cn.pledge.envconsole.book.web;

import cn.pledge.envconsole.book.model.param.RegisterParam;
import cn.pledge.envconsole.book.model.vo.RegisterVO;
import cn.pledge.envconsole.book.service.UserService;
import cn.pledge.envconsole.common.model.BaseResponse;
import cn.pledge.envconsole.common.utils.EthUtils;


import cn.pledge.envconsole.common.utils.TronUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
//import org.tron.common.utils.ByteArray;
import org.web3j.abi.FunctionEncoder;


import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

//import static org.tron.walletserver.WalletApi.decodeFromBase58Check;

/**
 * @author 89466
 */
@ApiSupport(order = 10)
@Api(tags = "H5用户注册页面")
@RequestMapping("/api/v1/user")
@RestController
@RequiredArgsConstructor
@CrossOrigin("*")
public class UserController {
    @Autowired
    private  UserService userService;
    @ApiOperation(value = "注册/登入")
    @PostMapping("/register")
    public BaseResponse<RegisterVO> register(@RequestBody RegisterParam param,@ApiIgnore HttpSession session) {
        RegisterVO register = userService.register(param,session);
        return BaseResponse.success(register);
    }

//      public static void main(String[] args) throws IOException {
//
//
////    {"address":"0xe81128942ed67a3b453576cad44fa9fb7f0b2098","privateKey":"8ca3edaabc0567d9555ade455bab24a27bea6ee0524e96ffac9a3cfc2b841214"}
//
//           String address = "0x8A88A6A613376562f83f36121D5470C1Afe6F15D";
//
//          String contract="0x55d398326f99059fF775485246999027B3197955";
//
//          Web3j web3j = null;
//          {
//              try{
//                  //如果这个地址不知道怎么获取 可以参考  https://blog.csdn.net/sail331x/article/details/115395131
//                  web3j = Web3j.build(new
//
//           HttpService("https://bsc-dataseed1.defibit.io/"));
//              }catch (Throwable t){
//                  t.printStackTrace();
//              }
//          }
//
//          BigInteger bigInteger = EthUtils.balanceOfErc20(web3j, contract, address).divide(new BigInteger("1000000000000"));
//  //        BigDecimal bigDecimal = EthUtils.balanceOf(web3j, address);
//          System.out.println(bigInteger);
//      }

//  public static void main(String[] args) {
//
//      String address = "0x8A88A6A613376562f83f36121D5470C1Afe6F15D";
////
//      String contractAddress="0x55d398326f99059fF775485246999027B3197955";
//      String methodName = "balanceOf";
//      List<Type> inputParameters = new ArrayList<>();
//      List<TypeReference<?>> outputParameters = new ArrayList<>();
//      Address fromAddress = new Address(address);
//      inputParameters.add(fromAddress);
//
//      TypeReference<Uint256> typeReference = new TypeReference<Uint256>() {
//      };
//      outputParameters.add(typeReference);
//      Function function = new Function(methodName, inputParameters, outputParameters);
//      String data = FunctionEncoder.encode(function);
//      Transaction transaction = Transaction.createEthCallTransaction(address, contractAddress, data);
//
//      EthCall ethCall;
//      Web3j web3j = null;
//              {
//                  try{
//                      //如果这个地址不知道怎么获取 可以参考  https://blog.csdn.net/sail331x/article/details/115395131
//                      web3j = Web3j.build(new
//
//       HttpService("https://bsc-dataseed1.binance.org/"));
//                  }catch (Throwable t){
//                      t.printStackTrace();
//                  }
//              }
//      BigDecimal balanceValue = BigDecimal.ZERO;
//      try {
//          ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.PENDING).send();
//          List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
//          Integer value = 0;
//          if(results != null && results.size()>0){
//              value = Integer.parseInt(String.valueOf(results.get(0).getValue()));
//          }
////          balanceValue = new BigDecimal(value).divide(WEI, 6, RoundingMode.HALF_DOWN);
//      } catch (IOException e) {
//          e.printStackTrace();
//      }
//
//  }
//  public static void main(String[] args) throws IOException {
//     String tronUrl = "https://api.trongrid.io";
//     String contract = "0x55d398326f99059fF775485246999027B3197955";
//     String address = "0x8A88A6A613376562f83f36121D5470C1Afe6F15D";
//
//      String hexAddress = address;
//
//      String hexContract = contract;
//
//    String url = tronUrl +"/wallet/triggerconstantcontract";
//    JSONObject param = new JSONObject();
//
//        param.put("owner_address", hexAddress);
//        param.put("contract_address", hexContract);
//        param.put("function_selector", "balanceOf(address)");
//    List<Type> inputParameters = new ArrayList<>();
//        inputParameters.add(new Address(hexAddress.substring(2)));
//        param.put("parameter", FunctionEncoder.encodeConstructor(inputParameters));
//    String result = HttpUtils.HttpPostWithJson(url, param.toJSONString());
//        if (StringUtils.isNotEmpty(result)) {
//        JSONObject obj = JSONObject.parseObject(result);
//        JSONArray results = obj.getJSONArray("constant_result");
//        if (results != null && results.size() > 0) {
//            BigInteger bigInteger = new BigInteger(results.getString(0).substring(2), 16);
//            BigInteger amount = new BigInteger(results.getString(0), 16);
//
//        }
//    }

//      TriggerSmartContract.Param param = new TriggerSmartContract.Param();
//      param.setContract_address(hexContract);
//      param.setOwner_address(hexAddress);
//      param.setFunction_selector("balanceOf(address)");
//      String addressParam = addZero(hexAddress.substring(2), 64);
//      param.setParameter(addressParam);
//      String url=tronUrl + "/wallet/triggersmartcontract";
//      TriggerSmartContract.Result result = restTemplate.postForEntity(url,param,TriggerSmartContract.Result.class).getBody();
//    if (result != null && result.isSuccess()) {
//      String value = result.getConstantResult(0);
//      if (value != null) {
//        final int accuracy = 6; // 六位小数
//        return new BigDecimal(value).divide(decimal, accuracy, RoundingMode.FLOOR);
//        // return new BigInteger(value, 16);
//      }
//          }
//
//  }
}
