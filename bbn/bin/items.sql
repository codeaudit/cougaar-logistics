select ownerid, "Items", count(*) from assetinstance_XXX where prototypeid not like "MOS%" group by ownerid;
