package com.example.paycheck;

public enum MoneyQuote {
    QUOTE_1("돈은 훌륭한 종이와 같습니다. 그것은 탁자 위에 있으면서도 곧 뛰어난 사람에 의해 계산됩니다. - Ayn Rand"),
    QUOTE_2("돈을 관리하지 못하면 돈이 당신을 관리합니다. - 그루치오 마르티노"),
    QUOTE_3("돈이 되는데 어려운 건 아니다. 다만 돈을 버는데 몇 가지 규칙이 있다. - 존 로크펠러"),
    QUOTE_4("돈은 세상에서 가장 달콤한 음식이 될 수 있고, 세상에서 가장 맛이 없는 약이 될 수도 있습니다. - 호라티우스"),
    QUOTE_5("부가 있으면 남에게 호의를 베풀 수 있지만 품위와 예의를 갖춰 베푸는 데는 부 이상의 것이 필요하다. - 찰스 칼렙 콜튼"),
    QUOTE_6("가장 적은 것으로도 만족하는 사람이 가장 부유한 사람이다. - 소크라테스");

    private final String quote;

    MoneyQuote(String quote) {
        this.quote = quote;
    }

    public String getQuote() {
        return quote;
    }
}
