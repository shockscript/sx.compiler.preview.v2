var sxrt = (function()
{
    var sxrt =
    {
        typeInfo: null
    }

    class TypeInfo
    {
        constructor()
        {
        }
    }

    sxrt.TypeInfo = TypeInfo

    return sxrt
})()

if (typeof exports != "undefined")
{
    exports.sxrt = sxrt
}
else if (typeof global != "undefined")
{
    global.sxrt = sxrt
}