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

    public static Flux<CouponDetailDto> getListFromBase64File(final String fileBase64) {
        return getCouponDetailDtoFlux(createFluxFrom(fileBase64));
    }

    private static Flux<CouponDetailDto> getCouponDetailDtoFlux(Flux<String> fileBase64) {
        AtomicInteger counter = new AtomicInteger(0);
        String characterSeparated = FileCSVEnum.CHARACTER_DEFAULT.getId();
        Set<String> codes = new HashSet<>();
        return          fileBase64
                        .map(line -> getTupleOfLine(line, line.split(characterSeparated), characterSeparated))
                        .map(tuple -> getCouponDetailDto(counter, codes, tuple));
    }

    private static CouponDetailDto getCouponDetailDto(AtomicInteger counter, Set<String> codes, Tuple2<String, String> tuple) {
        String errorMessage = getErrorMessage(tuple, codes);
        String dateValidated = getDateValidated(tuple, errorMessage);

        return CouponDetailDto.aCouponDetailDto()
                .withCode(evaluacionesDelBonoAnterior(tuple.getT1()))
                .withDueDate(dateValidated)
                .withNumberLine(counter.incrementAndGet())
                .withMessageError(errorMessage)
                .withTotalLinesFile(1)
                .build();
    }

    private static String getDateValidated(Tuple2<String, String> tuple, String errorMessage) {
        return (errorMessage == null) ? tuple.getT2() : null;
    }

    public static String getErrorMessage(Tuple2<String, String> tuple , Set<String> codes){
        Map<String, Boolean> error = new LinkedHashMap<>();
        error.put(ExperienceErrorsEnum.FILE_ERROR_COLUMN_EMPTY.toString(), isBlank(tuple));
        error.put(ExperienceErrorsEnum.FILE_ERROR_CODE_DUPLICATE.toString(), (!codes.add(tuple.getT1())));
        error.put(ExperienceErrorsEnum.FILE_ERROR_DATE_PARSE.toString(), (!validateDateRegex(tuple.getT2())));
        error.put(ExperienceErrorsEnum.FILE_DATE_IS_MINOR_OR_EQUALS.toString(), validateDateIsMinor(tuple.getT2()));
        for (Map.Entry<String, Boolean> errorbono : error.entrySet()){
            if(errorbono.getValue()){
                return errorbono.getKey();
            }
        }
        return null;
    }

    private static boolean isBlank(Tuple2<String, String> tuple) {
        return tuple.getT1().isBlank() || tuple.getT2().isBlank();
    }

    private static String evaluacionesDelBonoAnterior(String bonoEnviado){
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
        return bonoIn.chars().allMatch(Character::isDigit) && bonoLengthHigher(bonoIn.length(), 12)
                && bonoLengthSmaller(bonoIn.length(), 13);
    }
    
    private static boolean bonoStartWithAndLength(String bonoIn){
        return bonoIn.startsWith("*") && bonoLengthHigher(bonoIn.replace("*", "").length(),1)
                 && bonoLengthSmaller(bonoIn.replace("*", "").length(), 43);
    }

    private static boolean bonoLengthHigher(int bonoInLength, int number){
        return (bonoInLength >= number);
    }

    private static boolean bonoLengthSmaller(int bonoInLength, int number){
        return (bonoInLength <= number);
    }
    
    private static boolean validateDateRegex(String dateForValidate) {
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
                : lengthArray(array , 2)
                ? lineStartWith(characterSeparated, line)
                ? Tuples.of(EMPTY_STRING, array[0])
                : Tuples.of(array[0], EMPTY_STRING)
                : Tuples.of(array[0], array[1]);
    }

    private static boolean arrayCheck(String[] array){
        return (Objects.isNull(array)) || (array.length == 0);
    }
    
    private static boolean lengthArray(String[] array, int number){
        return array.length < number;
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
