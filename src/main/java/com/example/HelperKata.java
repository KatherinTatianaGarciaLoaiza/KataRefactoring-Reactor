package com.example;


import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class HelperKata {
    private static final  String EMPTY_STRING = "";
    private static String ANTERIOR_BONO = null;
    protected static String errorMessage = null;
    protected static String dateValidated = null;
    protected static String bonoEnviado = null;
    private static Set<String> codes = new HashSet<>();
    private static AtomicInteger counter = new AtomicInteger(0);


    public static Flux<CouponDetailDto> getListFromBase64File(final String fileBase64) {
        String characterSeparated = FileCSVEnum.CHARACTER_DEFAULT.getId();
        return createFluxFrom(fileBase64).map(cupon -> optionalCoupon(generateCoupon(cupon.split(characterSeparated))));
    }

    private static CouponDetailDto optionalCoupon(Coupon coupon){
       return Optional.of(coupon)
                .filter(HelperKata::emptyString)
                .map(cuponFilter -> errorEmpty())
               .orElseGet(() -> duplicateAndGetCouponDTO()
               );
    }

    private static CouponDetailDto duplicateAndGetCouponDTO() {
        validateDuplicate();
        return getCouponDetailDto();
    }

    private static CouponDetailDto errorEmpty() {
        errorMessage = ExperienceErrorsEnum.FILE_ERROR_COLUMN_EMPTY.toString();
        return getCouponDetailDto();
    }

    private static Coupon generateCoupon (String[] array){
        Coupon cupon = new Coupon(array[0], array[1]);
        return cupon;
    }

    private static void validateDuplicate(){
        if (!codes.add(bonoEnviado)){
            dateValidated = null;
            errorMessage = ExperienceErrorsEnum.FILE_ERROR_CODE_DUPLICATE.toString();
        }
    }

    private static CouponDetailDto getCouponDetailDto() {
        return CouponDetailDto.aCouponDetailDto()
                .withCode(evaluacionesDelBonoAnterior())
                .withDueDate(dateValidated)
                .withNumberLine(counter.incrementAndGet())
                .withMessageError(errorMessage)
                .withTotalLinesFile(1)
                .build();
    }

    private static boolean emptyString(Coupon cupon) {
        return cupon.getCode().equals(EMPTY_STRING) || cupon.getDate().equals(EMPTY_STRING);
    }

    private static String evaluacionesDelBonoAnterior(){
        return (ANTERIOR_BONO == null || ANTERIOR_BONO.equals("")) ? bonoVacioONullo(bonoEnviado) : bonoAnteriorIgualODiferenteAlEnviado(bonoEnviado);
    }

    private static String bonoVacioONullo(String bonoEnviado){
        ANTERIOR_BONO = typeBono(bonoEnviado);
        return  (ANTERIOR_BONO == "") ? null : bonoEnviado;
    }

    private static String bonoAnteriorIgualODiferenteAlEnviado(String bonoEnviado){
        return (ANTERIOR_BONO.equals(typeBono(bonoEnviado))) ? bonoEnviado : null;
    }

    private static Flux<String> createFluxFrom(String fileBase64) {
        return Flux.using(
                () -> new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(decodeBase64(fileBase64))
                )).lines().skip(1),
                Flux::fromStream,
                Stream::close
        );
    }

    private static String typeBono(String bonoIn) {
        return  bonoCharacterAndLength(bonoIn)
                ? ValidateCouponEnum.EAN_13.getTypeOfEnum()
                : bonoStartWithAndLength(bonoIn)
                ? ValidateCouponEnum.EAN_39.getTypeOfEnum()
                : ValidateCouponEnum.ALPHANUMERIC.getTypeOfEnum();
//        return Optional.of(bonoIn)
//                .filter(HelperKata::bonoCharacterAndLength)
//                .map(er -> ValidateCouponEnum.EAN_39.getTypeOfEnum())
//                .orElse(ValidateCouponEnum.ALPHANUMERIC.getTypeOfEnum());
    }

    private static boolean bonoCharacterAndLength(String bonoIn){
        return bonoIn.chars().allMatch(Character::isDigit) && validateMajorMinor(bonoIn.length(), 12)
                && validateMajorMinor(13, bonoIn.length());
    }
    
    private static boolean bonoStartWithAndLength(String bonoIn){
        return bonoIn.startsWith("*") && validateMajorMinor(bonoIn.replace("*", "").length(),1)
                 && validateMajorMinor(43, bonoIn.replace("*", "").length());
    }

    private static boolean validateMajorMinor(int biggerNumber, int minorNumber){
        return (biggerNumber >= minorNumber);
    }
    
    static boolean validateDateRegex(String dateForValidate) {
        String regex = FileCSVEnum.PATTERN_DATE_DEFAULT.getId();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(dateForValidate);
        return matcher.matches();
    }

    private static byte[] decodeBase64(final String fileBase64) {
        return Base64.getDecoder().decode(fileBase64);
    }

    private static Tuple2<String, String> getTupleOfLine(String line, String[] array, String characterSeparated) {
        return  arrayCheck(array)
                ? Tuples.of(EMPTY_STRING, EMPTY_STRING)
                : validateMajorMinor(1, array.length)
                ? lineStartWith(characterSeparated, line)
                ? Tuples.of(EMPTY_STRING, array[0])
                : Tuples.of(array[0], EMPTY_STRING)
                : Tuples.of(array[0], array[1]);
    }

    private static boolean arrayCheck(String[] array){
        return (Objects.isNull(array)) || (array.length == 0);
    }

    private static boolean lineStartWith(String characterSeparated, String line){
        return line.startsWith(characterSeparated);
    }

    public static boolean validateDateIsMinor(String dateForValidate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(FileCSVEnum.PATTERN_SIMPLE_DATE_FORMAT.getId());
            Date dateActual = sdf.parse(sdf.format(new Date()));
            Date dateCompare = sdf.parse(dateForValidate);
            return ((dateCompare.compareTo(dateActual)) <= 0);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
