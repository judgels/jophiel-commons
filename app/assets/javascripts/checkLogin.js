require(["jquery"], function( __jquery__ ) {
    $(document).ready(function () {
        $("#check-login").load(function() {
            $.get(checkLoginUrl, function(data) {
                if (!data.success) {
                    alert(confirmMessage);
                    location.reload();
                }
            }, "json");
        });
        setInterval(function () {
            $("#check-login").attr('src', $("#check-login").attr('src'));
        }, checkInterval);
    });
});
